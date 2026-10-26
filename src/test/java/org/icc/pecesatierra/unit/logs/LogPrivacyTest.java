package org.icc.pecesatierra.unit.logs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.icc.pecesatierra.configurations.SqlQueryValidator;
import org.icc.pecesatierra.features.ai.dtos.AiChatRequestDto;
import org.icc.pecesatierra.features.ai.service.AiService;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.features.branch.repository.BranchRepository;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.category.repository.mapper.MemberNotesMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.MemberNotes;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.features.member.repository.MemberNotesRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberNoteService;
import org.icc.pecesatierra.features.member.service.MemberPersistenceService;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.MemberCategoryDeterminer;
import org.icc.pecesatierra.utils.enums.Gender;
import org.icc.pecesatierra.utils.sql_executor.SqlExecutor;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * H-13: los logs INFO de integrantes, notas pastorales y AI no pueden contener
 * datos personales. Se capturan con un {@link ListAppender} de Logback y se
 * comprueba que ni el mensaje formateado ni los argumentos exponen el contenido
 * sensible, sin depender del formato exacto de cada linea.
 */
class LogPrivacyTest {

    private final Branch branch = TestFixtures.branch("branch-a", "Sede A");
    private final Member member = TestFixtures.member("member-1", branch, "Ana Confidencial");
    private final User operator = TestFixtures.user("user-1", "operador-secreto", member);

    @Test
    void memberNoteLogsDoNotExposeThePastoralNote_H13() {
        DateTimeUtils dateTimeUtils = mock(DateTimeUtils.class);
        MemberNotesMapper mapper = mock(MemberNotesMapper.class);
        MemberNotesRepository notesRepository = mock(MemberNotesRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);

        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        doAnswer(invocation -> {
            MemberNotes saved = invocation.getArgument(0);
            saved.setId("note-1");
            return saved;
        }).when(notesRepository).save(any(MemberNotes.class));

        MemberNoteService service = new MemberNoteService(
                dateTimeUtils, mapper, notesRepository, memberRepository);

        ListAppender<ILoggingEvent> logs = attach(MemberNoteService.class);
        try {
            service.createNote(MemberNoteRequestDto.builder()
                    .memberId(member.getId())
                    .note("Nota pastoral secreta sobre una crisis familiar")
                    .build(), operator);
        } finally {
            detach(MemberNoteService.class, logs);
        }

        assertThat(loggedText(logs))
                .doesNotContain("Ana Confidencial")
                .doesNotContain("Nota pastoral secreta")
                .doesNotContain("operador-secreto")
                .contains("note-1")
                .contains(member.getId());
    }

    @Test
    void memberPersistenceLogsDoNotExposePii_H13() {
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberMapper memberMapper = mock(MemberMapper.class);
        MemberCategoryRepository categoryRepository = mock(MemberCategoryRepository.class);
        MemberTypeRepository typeRepository = mock(MemberTypeRepository.class);
        MemberSubCategoryRepository subCategoryRepository = mock(MemberSubCategoryRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        MemberCategoryDeterminer determiner = mock(MemberCategoryDeterminer.class);
        DateTimeUtils dateTimeUtils = mock(DateTimeUtils.class);

        MemberCategory category = MemberCategory.builder()
                .id("cat-1").name("Categoria").color("#112233").build();
        MemberType type = MemberType.builder()
                .id("type-1").name("Tipo").color("#112233").build();

        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(typeRepository.findById("type-1")).thenReturn(Optional.of(type));
        doAnswer(invocation -> {
            Member saved = invocation.getArgument(0);
            saved.setId("member-new");
            return saved;
        }).when(memberRepository).save(any(Member.class));

        MemberPersistenceService service = new MemberPersistenceService(
                memberRepository, memberMapper, categoryRepository, typeRepository,
                subCategoryRepository, branchRepository, determiner, dateTimeUtils);

        MemberRequestDto dto = MemberRequestDto.builder()
                .completeName("Ana Confidencial")
                .cc("1000000001")
                .cellphone("3001112233")
                .city("Bogota Secreta")
                .neighborhood("Barrio Secreto")
                .typeId("type-1")
                .categoryId("cat-1")
                .gender(Gender.MUJER)
                .build();

        ListAppender<ILoggingEvent> logs = attach(MemberPersistenceService.class);
        try {
            service.save(dto, null, operator);
        } finally {
            detach(MemberPersistenceService.class, logs);
        }

        assertThat(loggedText(logs))
                .doesNotContain("Ana Confidencial")
                .doesNotContain("1000000001")
                .doesNotContain("3001112233")
                .doesNotContain("Bogota Secreta")
                .doesNotContain("Barrio Secreto")
                .contains("member-new")
                .contains(branch.getId());
    }

    @Test
    void aiLogsDoNotExposeTheGeneratedSqlOrTheModelResponse_H13() {
        SqlExecutor sqlExecutor = mock(SqlExecutor.class);
        SqlQueryValidator validator = mock(SqlQueryValidator.class);
        RestTemplate restTemplate = mock(RestTemplate.class);

        String generated = "```sql\nSELECT complete_name, cellphone FROM members\n```";
        String formatted = "{\"answer\":\"Hay dos integrantes\",\"chartData\":null}";

        when(validator.isSafeSelect(anyString())).thenReturn(true);
        when(sqlExecutor.sqlExecutor(anyString())).thenReturn(List.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(geminiResponse(generated), geminiResponse(formatted));

        AiService service = new AiService(sqlExecutor, validator, restTemplate, new ObjectMapper());

        ListAppender<ILoggingEvent> logs = attach(AiService.class);
        try {
            service.processChat(AiChatRequestDto.builder().message("integrantes").build(), operator);
        } finally {
            detach(AiService.class, logs);
        }

        assertThat(loggedText(logs))
                .doesNotContain("SELECT complete_name")
                .doesNotContain("cellphone")
                .doesNotContain("Hay dos integrantes");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> geminiResponse(String text) {
        Map<String, Object> body = Map.of("candidates",
                List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", text))))));
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    private ListAppender<ILoggingEvent> attach(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        return appender;
    }

    private void detach(Class<?> type, ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(type)).detachAppender(appender);
        appender.stop();
    }

    /**
     * Mensaje formateado + argumentos: si un objeto sensible se pasara como
     * parametro, apareceria aqui aunque el patron no lo mostrara.
     */
    private String loggedText(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .map(event -> event.getFormattedMessage() + " | "
                        + (event.getArgumentArray() == null ? "" :
                        Arrays.stream(event.getArgumentArray())
                                .map(String::valueOf)
                                .collect(Collectors.joining(" "))))
                .collect(Collectors.joining("\n"));
    }
}
