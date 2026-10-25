package org.icc.pecesatierra.unit.advice;

import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M-04 y M-14: los 4xx de Spring MVC se prueban a traves del pipeline real de
 * MockMvc (handler mapping, adaptador y resolucion de excepciones), no llamando
 * al handler a mano. Si una anotacion {@code @ExceptionHandler} se repunta a otra
 * excepcion, estos tests fallan.
 */
class GlobalHandlerExceptionMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void missingRequiredRequestParamIs400_M04() throws Exception {
        mockMvc.perform(get("/api/probe/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("El parámetro 'value' es obligatorio."));
    }

    @Test
    void unsupportedHttpMethodIs405_M04() throws Exception {
        mockMvc.perform(put("/api/probe/upload"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(HttpStatus.METHOD_NOT_ALLOWED.value()))
                .andExpect(jsonPath("$.message").value("El método HTTP no está soportado para esta ruta."));
    }

    @Test
    void unsupportedMediaTypeIs415_M04() throws Exception {
        mockMvc.perform(post("/api/probe/required")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("texto plano"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
                .andExpect(jsonPath("$.message").value("El tipo de contenido no está soportado."));
    }

    @Test
    void unreadableJsonBodyIs400_M14() throws Exception {
        mockMvc.perform(post("/api/probe/required")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la petición es inválido o está mal formado."));
    }

    @Test
    void bindingFailureOnAModelAttributeIs400_M14() throws Exception {
        mockMvc.perform(get("/api/probe/model").param("age", "no-es-un-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void bindExceptionIs400_M14() throws Exception {
        mockMvc.perform(get("/api/probe/bind"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("La fecha es obligatoria"));
    }

    @Test
    void invalidSortPropertyIs400_M14() throws Exception {
        mockMvc.perform(get("/api/probe/sort"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("El campo de ordenamiento no es válido."));
    }

    @Test
    void missingResourceIs404AndNot500_M04() throws Exception {
        mockMvc.perform(get("/api/probe/missing-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").value("El recurso solicitado no existe."));
    }

    @Test
    void maxUploadSizeExceededIs413_M04() throws Exception {
        mockMvc.perform(post("/api/probe/upload-exceeded"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(HttpStatus.PAYLOAD_TOO_LARGE.value()))
                .andExpect(jsonPath("$.message").value("El archivo supera el tamaño máximo permitido."));
    }

    @Test
    void dataIntegrityViolationIs409OverTheRealMvcPipeline_H42() throws Exception {
        mockMvc.perform(post("/api/probe/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("La operación entra en conflicto con datos existentes."))
                .andExpect(jsonPath("$.path").value("/api/probe/integrity"));
    }

    @RestController
    @RequestMapping("/api/probe")
    static class ProbeController {

        @GetMapping("/required")
        String required(@RequestParam("value") String value) {
            return value;
        }

        @PostMapping("/required")
        String withBody(@RequestBody Map<String, String> body) {
            return "ok";
        }

        @GetMapping("/model")
        String model(@ModelAttribute ModelForm form) {
            return "ok";
        }

        @GetMapping("/sort")
        String sort() {
            throw new PropertyReferenceException("campoInexistente", TypeInformation.of(String.class), List.of());
        }

        @GetMapping("/bind")
        String bind() throws BindException {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "dto");
            bindingResult.addError(new FieldError("dto", "startDate", "La fecha es obligatoria"));
            throw new BindException(bindingResult);
        }

        @GetMapping("/missing-resource")
        String missingResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "api/probe/no-existe");
        }

        @PostMapping("/upload")
        String upload() {
            return "ok";
        }

        @PostMapping("/upload-exceeded")
        String uploadExceeded() {
            throw new MaxUploadSizeExceededException(5_242_880L);
        }

        @PostMapping("/integrity")
        String integrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"member_types_name_key\"");
        }
    }

    public static class ModelForm {

        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
