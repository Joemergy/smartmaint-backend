package com.smartmaint.backend;

import com.smartmaint.controller.TareaController;
import com.smartmaint.backend.config.TestSecurityConfig;
import com.smartmaint.service.TareaService;
import com.smartmaint.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TareaController.class)
@Import(TestSecurityConfig.class) // ✅ usamos la config de pruebas
@AutoConfigureMockMvc(addFilters = false) // ✅ desactiva filtros de seguridad
class TareaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TareaService tareaService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void crearTareaEndpoint_debeResponder201() throws Exception {
        mockMvc.perform(multipart("/api/tareas")
                .param("titulo", "Prueba título")
                .param("descripcion", "Prueba descripción")
                .param("fechaInicio", "2026-02-25T18:50:00")
                .param("nombreMaquina", "Compresor")
                .param("estado", "Pendiente")
                .param("prioridad", "ALTA")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void listarTareas_debeResponder200() throws Exception {
        mockMvc.perform(get("/api/tareas")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void actualizarEstado_conEstadoVacio_debeResponder400() throws Exception {
        mockMvc.perform(put("/api/tareas/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
