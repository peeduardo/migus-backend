package br.com.senac.migus.service;

import br.com.senac.migus.exception.AuthException;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.mongodb.repository.Query;
import org.bson.Document;
import org.springframework.data.mongodb.util.json.ParameterBindingDocumentCodec;
import br.com.senac.migus.dto.DadosPessoaisExportDTO;
import br.com.senac.migus.dto.UsuarioDTO;
import br.com.senac.migus.dto.UsuarioResponseDTO;
import br.com.senac.migus.dto.UsuarioUpdateRequestDTO;
import br.com.senac.migus.exception.ResourceNotFoundException;
import br.com.senac.migus.model.Inscricao;
import br.com.senac.migus.model.Programa;
import br.com.senac.migus.model.Usuarios;
import br.com.senac.migus.repository.InscricaoRepository;
import br.com.senac.migus.repository.ProgramaRepository;
import br.com.senac.migus.repository.UsuariosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuariosServiceTest {

    @Mock
    private UsuariosRepository usuariosRepository;

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private ProgramaRepository programaRepository;

    @InjectMocks
    private UsuariosService usuariosService;

    private Usuarios usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioExistente = new Usuarios(
                "João Silva",
                "joao@email.com",
                "hash123",
                "12345678901",
                "11988887777",
                LocalDate.of(1995, 3, 15),
                "VOLUNTARIO",
                true,
                LocalDate.of(2026, 1, 1),
                null
        );
        usuarioExistente.setId("user-123");
    }

    @Test
    void atualizacoesParciaisPreservamHistoricoEEdicaoNaoEscreveStatus() throws Exception {
        var codec = new ParameterBindingDocumentCodec();
        var desativar = UsuariosRepository.class.getMethod("desativarParticipante", String.class, String.class, LocalDate.class);
        Object[] dados = {"usuario-1", "VOLUNTARIO", "2026-11-10"};
        var filtro = codec.decode(desativar.getAnnotation(Query.class).value(), dados);
        var escrita = codec.decode(desativar.getAnnotation(Update.class).value(), dados);
        assertThat(filtro).isEqualTo(Document.parse("{'_id':'usuario-1','ativo':true,'tipoUsuario':'VOLUNTARIO'}"));
        assertThat(escrita).isEqualTo(Document.parse("{'$set':{'ativo':false,'dataAtualizacao':'2026-11-10'}}"));

        var editar = UsuariosRepository.class.getMethod("atualizarDadosPessoais", String.class, String.class,
                String.class, String.class, LocalDate.class, LocalDate.class);
        Object[] pessoais = {"usuario-1", "Ana", "ana@exemplo.invalid", null, null, "2026-11-10"};
        var filtroEdicao = codec.decode(editar.getAnnotation(Query.class).value(), pessoais);
        var escritaEdicao = codec.decode(editar.getAnnotation(Update.class).value(), pessoais);
        assertThat(filtroEdicao).isEqualTo(Document.parse("{'_id':'usuario-1','ativo':true}"));
        assertThat(escritaEdicao.keySet()).containsExactly("$set");
        assertThat(escritaEdicao.get("$set", Document.class).keySet())
                .containsExactlyInAnyOrder("nome", "email", "telefone", "dataNascimento", "dataAtualizacao");
    }

    @Test
    void edicaoIniciadaAntesDaDesativacaoNaoPodeRegravarAConta() {
        var dto = new UsuarioUpdateRequestDTO("João", usuarioExistente.getEmail(), null, null, null, null);
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        // A leitura viu a conta ativa, mas a escrita condicional já não encontrou a conta.
        when(usuariosRepository.atualizarDadosPessoais(any(), any(), any(), any(), any(), any())).thenReturn(0L);
        when(usuariosRepository.existsByIdAndAtivoTrue("user-123")).thenReturn(false);
        assertThatThrownBy(() -> usuariosService.atualizar("user-123", dto, "user-123"))
                .isInstanceOf(AuthException.class);
        verify(usuariosRepository, never()).save(any());
    }

    @Test
    void deveAtualizarUsuarioComSucesso() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João da Silva Atualizado",
                "joao.novo@email.com",
                "11999998888",
                LocalDate.of(1995, 3, 20),
                null,
                null
        );

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.findByEmail("joao.novo@email.com")).thenReturn(Optional.empty());
        when(usuariosRepository.atualizarDadosPessoais(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        UsuarioResponseDTO response = usuariosService.atualizar("user-123", dto, "user-123");

        assertThat(response).isNotNull();
        assertThat(response.mensagem()).isEqualTo("Usuário atualizado com sucesso!");
        assertThat(response.usuario().id()).isEqualTo("user-123");
        assertThat(response.usuario().nome()).isEqualTo("João da Silva Atualizado");
        assertThat(response.usuario().email()).isEqualTo("joao.novo@email.com");
        assertThat(response.usuario().telefone()).isEqualTo("11999998888");
        assertThat(response.usuario().dataNascimento()).isEqualTo(LocalDate.of(1995, 3, 20));
        assertThat(response.usuario().tipoUsuario()).isEqualTo("VOLUNTARIO");
        assertThat(response.usuario().ativo()).isTrue();
        assertThat(response.usuario().dataAtualizacao()).isEqualTo(LocalDate.now());

        verify(usuariosRepository).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    @Test
    void devePermitirAtualizarMantendoOMesmoEmail() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João Alterado",
                "joao@email.com",
                "11977776666",
                null,
                null,
                null
        );

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.atualizarDadosPessoais(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        UsuarioResponseDTO response = usuariosService.atualizar("user-123", dto, "user-123");

        assertThat(response.mensagem()).isEqualTo("Usuário atualizado com sucesso!");
        assertThat(response.usuario().nome()).isEqualTo("João Alterado");
        assertThat(response.usuario().email()).isEqualTo("joao@email.com");
        assertThat(response.usuario().telefone()).isEqualTo("11977776666");

        verify(usuariosRepository, never()).findByEmail(any());
        verify(usuariosRepository).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "Inexistente",
                "inexistente@email.com",
                "11900000000",
                null,
                null,
                null
        );

        when(usuariosRepository.findById("id-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.atualizar("id-invalido", dto, "id-invalido"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado com o ID: id-invalido");

        verify(usuariosRepository, never()).save(any());
        verify(usuariosRepository, never()).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deveLancarExcecaoQuandoEmailPertenceAOutroUsuario() {
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "João Silva",
                "outro@email.com",
                "11988887777",
                null,
                null,
                null
        );

        Usuarios outroUsuario = new Usuarios();
        outroUsuario.setId("user-456");
        outroUsuario.setEmail("outro@email.com");

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.findByEmail("outro@email.com")).thenReturn(Optional.of(outroUsuario));

        assertThatThrownBy(() -> usuariosService.atualizar("user-123", dto, "user-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O e-mail informado já está em uso por outro usuário");

        verify(usuariosRepository, never()).save(any());
        verify(usuariosRepository, never()).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getMeuPerfil_deveRetornarDadosDoUsuario() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));

        UsuarioDTO dto = usuariosService.getMeuPerfil("user-123");

        assertThat(dto.id()).isEqualTo("user-123");
        assertThat(dto.nome()).isEqualTo("João Silva");
        assertThat(dto.email()).isEqualTo("joao@email.com");
    }

    @Test
    void getMeuPerfil_deveLancarExcecaoSeNaoEncontrado() {
        when(usuariosRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.getMeuPerfil("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    void getMeuPerfil_naoDeveExporSenhaHash() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));

        UsuarioDTO dto = usuariosService.getMeuPerfil("user-123");

        boolean temCampoSenha = java.util.Arrays.stream(dto.getClass().getRecordComponents())
                .anyMatch(c -> c.getName().toLowerCase().contains("senha"));

        assertThat(temCampoSenha).isFalse();
    }

    private Usuarios conta(String id, String perfil, boolean ativo) {
        var usuario = new Usuarios();
        usuario.setId(id);
        usuario.setTipoUsuario(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }

    @Test
    void deveInativarVoluntarioAtivoPreservandoOsDemaisDados() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.desativarParticipante(eq("user-123"), eq("VOLUNTARIO"), any())).thenReturn(1L);

        UsuarioResponseDTO response = usuariosService.inativarVoluntario("user-123", "resp-1");

        assertThat(response.mensagem()).isEqualTo("Cadastro inativado com sucesso!");
        assertThat(response.usuario().id()).isEqualTo("user-123");
        assertThat(response.usuario().ativo()).isFalse();
        assertThat(response.usuario().dataAtualizacao()).isEqualTo(LocalDate.now());
        assertThat(response.usuario().nome()).isEqualTo("João Silva");
        assertThat(response.usuario().email()).isEqualTo("joao@email.com");
        assertThat(response.usuario().cpf()).isEqualTo("12345678901");
        assertThat(response.usuario().tipoUsuario()).isEqualTo("VOLUNTARIO");
        assertThat(response.usuario().dataCadastro()).isEqualTo(LocalDate.of(2026, 1, 1));
        verify(usuariosRepository).desativarParticipante("user-123", "VOLUNTARIO", LocalDate.now());
        verify(usuariosRepository, never()).save(any());
        verify(usuariosRepository, never()).deleteById(any());
    }

    @Test
    void deveInativarCadastroDeInteressado() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("int-1")).thenReturn(Optional.of(conta("int-1", "interessado", true)));
        when(usuariosRepository.desativarParticipante(eq("int-1"), eq("interessado"), any())).thenReturn(1L);

        UsuarioResponseDTO response = usuariosService.inativarVoluntario("int-1", "resp-1");

        assertThat(response.usuario().ativo()).isFalse();
    }

    @Test
    void deveExigirAutenticacaoParaInativar() {
        assertThatThrownBy(() -> usuariosService.inativarVoluntario("user-123", null))
                .isInstanceOf(AuthException.class)
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void deveBloquearInativacaoPorSolicitanteInexistente() {
        when(usuariosRepository.findById("sumiu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("user-123", "sumiu"))
                .isInstanceOf(AuthException.class)
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"interessado,true", "VOLUNTARIO,true", "RESPONSAVEL,false"})
    void deveBloquearInativacaoPorUsuarioSemAutorizacao(String perfil, boolean ativo) {
        when(usuariosRepository.findById("solicitante")).thenReturn(Optional.of(conta("solicitante", perfil, ativo)));

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("user-123", "solicitante"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Somente um responsável ativo pode inativar cadastros")
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void deveTratarInativacaoDeCadastroInexistente() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("inexistente", "resp-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Voluntário não encontrado com o ID: inexistente");
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void naoDeveInativarCadastroDeOutroResponsavel() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("resp-2")).thenReturn(Optional.of(conta("resp-2", "RESPONSAVEL", true)));

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("resp-2", "resp-1"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Somente cadastros de voluntários podem ser inativados")
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void deveTratarCadastroJaInativo() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("vol-1")).thenReturn(Optional.of(conta("vol-1", "VOLUNTARIO", false)));

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("vol-1", "resp-1"))
                .isInstanceOf(AuthException.class)
                .hasMessage("O cadastro já está inativo")
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        verify(usuariosRepository, never()).desativarParticipante(any(), any(), any());
    }

    @Test
    void deveTratarCadastroInativadoDuranteAOperacao() {
        when(usuariosRepository.findById("resp-1")).thenReturn(Optional.of(conta("resp-1", "RESPONSAVEL", true)));
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.desativarParticipante(eq("user-123"), eq("VOLUNTARIO"), any())).thenReturn(0L);

        assertThatThrownBy(() -> usuariosService.inativarVoluntario("user-123", "resp-1"))
                .isInstanceOf(AuthException.class)
                .extracting(erro -> ((AuthException) erro).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
    }

    @Test
    void desativarMinhaConta_deveLancarExcecaoSeDesativarRetornaZero() {
        var usuario = conta("user-1", "VOLUNTARIO", true);
        when(usuariosRepository.findById("user-1")).thenReturn(Optional.of(usuario));
        when(usuariosRepository.desativarParticipante(eq("user-1"), eq("VOLUNTARIO"), any())).thenReturn(0L);

        assertThatThrownBy(() -> usuariosService.desativarMinhaConta("user-1"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void atualizar_deveLancarExcecaoSeAutenticacaoNula() {
        var dto = new UsuarioUpdateRequestDTO("Ana", "ana@email.com", null, null, null, null);
        assertThatThrownBy(() -> usuariosService.atualizar("user-1", dto, null))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
        verify(usuariosRepository, never()).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    @Test
    void atualizar_naoDeveLancarExcecaoSeEmailPertenceAoMesmoUsuario() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        var outro = new Usuarios();
        outro.setId("user-123"); // mesmo ID — não é duplicata
        when(usuariosRepository.findByEmail("novo@email.com")).thenReturn(Optional.of(outro));
        when(usuariosRepository.atualizarDadosPessoais(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        var dto = new UsuarioUpdateRequestDTO("João Silva", "novo@email.com", null, null, null, null);
        var resp = usuariosService.atualizar("user-123", dto, "user-123");

        assertThat(resp).isNotNull();
    }

    @Test
    void atualizar_deveLancarExcecaoParaIdadeMenorQue17Anos() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        LocalDate menorDeIdade = LocalDate.now().minusYears(16);
        var dto = new UsuarioUpdateRequestDTO("João", "joao@email.com", null, menorDeIdade, null, null);

        assertThatThrownBy(() -> usuariosService.atualizar("user-123", dto, "user-123"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        verify(usuariosRepository, never()).atualizarDadosPessoais(any(), any(), any(), any(), any(), any());
    }

    // --- exportarDados ---

    @Test
    void exportarDados_deveRetornarDadosComInscricoes() {
        var programa = new Programa("prog-1", "Apoio", "Desc", java.util.List.of("Educação"),
                java.util.List.of("segunda"), "09:00", "12:00", true, "resp-1", new java.util.Date(), null);
        var inscricao = new Inscricao("insc-1", "user-123", "prog-1", "APROVADA",
                new java.util.Date(), new java.util.Date(), null, null);

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(inscricaoRepository.findByVoluntarioId("user-123")).thenReturn(java.util.List.of(inscricao));
        when(programaRepository.findAllById(java.util.List.of("prog-1"))).thenReturn(java.util.List.of(programa));

        DadosPessoaisExportDTO resultado = usuariosService.exportarDados("user-123");

        assertThat(resultado.id()).isEqualTo("user-123");
        assertThat(resultado.nome()).isEqualTo("João Silva");
        assertThat(resultado.email()).isEqualTo("joao@email.com");
        assertThat(resultado.cpf()).isEqualTo("12345678901");
        assertThat(resultado.inscricoes()).hasSize(1);
        assertThat(resultado.inscricoes().get(0).programaNome()).isEqualTo("Apoio");
        assertThat(resultado.inscricoes().get(0).status()).isEqualTo("APROVADA");
    }

    @Test
    void exportarDados_semInscricoes_deveRetornarListaVazia() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(inscricaoRepository.findByVoluntarioId("user-123")).thenReturn(java.util.List.of());
        when(programaRepository.findAllById(java.util.List.of())).thenReturn(java.util.List.of());

        DadosPessoaisExportDTO resultado = usuariosService.exportarDados("user-123");

        assertThat(resultado.inscricoes()).isEmpty();
    }

    @Test
    void exportarDados_deveRetornarNullParaNomeDeProgamaNaoEncontrado() {
        var inscricao = new Inscricao("insc-1", "user-123", "prog-inexistente", "PENDENTE",
                new java.util.Date(), null, null, null);
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(inscricaoRepository.findByVoluntarioId("user-123")).thenReturn(java.util.List.of(inscricao));
        when(programaRepository.findAllById(any())).thenReturn(java.util.List.of());

        DadosPessoaisExportDTO resultado = usuariosService.exportarDados("user-123");

        assertThat(resultado.inscricoes()).hasSize(1);
        assertThat(resultado.inscricoes().get(0).programaNome()).isNull();
    }

    @Test
    void exportarDados_deveLancarExcecaoSeUsuarioNaoEncontrado() {
        when(usuariosRepository.findById("sumiu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.exportarDados("sumiu"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- anonimizarMinhaConta ---

    @Test
    void anonimizarMinhaConta_deveAnonimizarERetornarSucesso() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.anonimizarUsuario(eq("user-123"), any(), eq("00000000000"), any())).thenReturn(1L);

        UsuarioResponseDTO resposta = usuariosService.anonimizarMinhaConta("user-123");

        assertThat(resposta.mensagem()).isEqualTo("Dados pessoais anonimizados com sucesso.");
        assertThat(resposta.usuario().nome()).isEqualTo("Usuário Removido");
        assertThat(resposta.usuario().cpf()).isEqualTo("00000000000");
        assertThat(resposta.usuario().email()).contains("@anonimizado.invalid");
        assertThat(resposta.usuario().telefone()).isNull();
        assertThat(resposta.usuario().dataNascimento()).isNull();
        assertThat(resposta.usuario().ativo()).isFalse();
    }

    @Test
    void anonimizarMinhaConta_deveLancarExcecaoSeUsuarioNaoEncontrado() {
        when(usuariosRepository.findById("sumiu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuariosService.anonimizarMinhaConta("sumiu"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonimizarMinhaConta_deveLancarExcecaoSeAtualizacaoNaoAfetouRegistro() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.anonimizarUsuario(any(), any(), any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> usuariosService.anonimizarMinhaConta("user-123"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonimizarMinhaConta_deveGerarEmailUnico() {
        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.anonimizarUsuario(eq("user-123"), any(), any(), any())).thenReturn(1L);

        var r1 = usuariosService.anonimizarMinhaConta("user-123");

        when(usuariosRepository.findById("user-123")).thenReturn(Optional.of(usuarioExistente));
        when(usuariosRepository.anonimizarUsuario(eq("user-123"), any(), any(), any())).thenReturn(1L);
        var r2 = usuariosService.anonimizarMinhaConta("user-123");

        assertThat(r1.usuario().email()).isNotEqualTo(r2.usuario().email());
    }
}
