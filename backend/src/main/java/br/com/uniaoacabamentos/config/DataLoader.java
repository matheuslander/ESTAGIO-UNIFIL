package br.com.uniaoacabamentos.config;

import br.com.uniaoacabamentos.model.Material;
import br.com.uniaoacabamentos.model.Obra;
import br.com.uniaoacabamentos.model.StatusObra;
import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.MaterialRepository;
import br.com.uniaoacabamentos.repository.ObraRepository;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final ObraRepository obraRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UsuarioRepository usuarioRepository,
                      MaterialRepository materialRepository,
                      ObraRepository obraRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.materialRepository = materialRepository;
        this.obraRepository = obraRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setLogin("admin");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
            usuarioRepository.save(admin);

            Usuario comum = new Usuario();
            comum.setNome("Usuário Comum");
            comum.setLogin("usuario");
            comum.setSenha(passwordEncoder.encode("usuario123"));
            comum.setTipoUsuario(TipoUsuario.USUARIO_COMUM);
            usuarioRepository.save(comum);
        } else {
            usuarioRepository.findAll().forEach(usuario -> {
                if (!isSenhaComHash(usuario.getSenha())) {
                    usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
                    usuarioRepository.save(usuario);
                }
            });
        }

        if (materialRepository.count() == 0) {
            Material piso = new Material();
            piso.setNome("Piso Vinílico");
            piso.setDescricao("Piso vinílico para obras.");
            piso.setMarca("Durafloor");
            piso.setCor("Carvalho");
            piso.setQuantidade(80);
            piso.setEstoqueMinimo(20);
            piso.setValorCusto(new BigDecimal("45.90"));
            piso.setValorVenda(new BigDecimal("69.90"));
            materialRepository.save(piso);

            Material la = new Material();
            la.setNome("Lã de Vidro");
            la.setDescricao("Isolamento acústico.");
            la.setMarca("Isover");
            la.setCor("Amarela");
            la.setQuantidade(8);
            la.setEstoqueMinimo(10);
            la.setValorCusto(new BigDecimal("25.00"));
            la.setValorVenda(new BigDecimal("39.90"));
            materialRepository.save(la);
        }

        if (obraRepository.count() == 0) {
            Obra obra = new Obra();
            obra.setNomeObra("Apartamento Centro");
            obra.setNomeCliente("João Silva");
            obra.setEndereco("Rua Exemplo, 123");
            obra.setDescricao("Instalação de piso vinílico.");
            obra.setMetragem(65.0);
            obra.setStatus(StatusObra.EM_ANDAMENTO);
            obraRepository.save(obra);
        }
    }

    private boolean isSenhaComHash(String senha) {
        return senha != null && (
                senha.startsWith("$2a$") ||
                        senha.startsWith("$2b$") ||
                        senha.startsWith("$2y$")
        );
    }
}
