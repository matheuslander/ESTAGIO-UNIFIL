package br.com.uniaoacabamentos.config;

import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.MaterialRepository;
import br.com.uniaoacabamentos.repository.ObraRepository;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga de dados exclusivamente para desenvolvimento e demonstracao academica.
 * Pode ser desativada com UNICONTROL_DEV_DATA_ENABLED=false.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(
        name = "unicontrol.dev-data.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DataLoader implements CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    private final MaterialRepository materialRepository;
    private final ObraRepository obraRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminLogin;
    private final String adminPassword;
    private final String userLogin;
    private final String userPassword;

    public DataLoader(UsuarioRepository usuarioRepository,
                      MaterialRepository materialRepository,
                      ObraRepository obraRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${unicontrol.dev-data.admin.login:admin}") String adminLogin,
                      @Value("${unicontrol.dev-data.admin.password:admin123}") String adminPassword,
                      @Value("${unicontrol.dev-data.user.login:usuario}") String userLogin,
                      @Value("${unicontrol.dev-data.user.password:usuario123}") String userPassword) {
        this.usuarioRepository = usuarioRepository;
        this.materialRepository = materialRepository;
        this.obraRepository = obraRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminLogin = adminLogin;
        this.adminPassword = adminPassword;
        this.userLogin = userLogin;
        this.userPassword = userPassword;
    }

    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setLogin(adminLogin.trim().toLowerCase());
            admin.setSenha(passwordEncoder.encode(adminPassword));
            admin.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
            admin.setAtivo(true);
            usuarioRepository.save(admin);

            Usuario comum = new Usuario();
            comum.setNome("Usuário Comum");
            comum.setLogin(userLogin.trim().toLowerCase());
            comum.setSenha(passwordEncoder.encode(userPassword));
            comum.setTipoUsuario(TipoUsuario.USUARIO);
            comum.setAtivo(true);
            usuarioRepository.save(comum);
        } else {
            usuarioRepository.findAll().forEach(usuario -> {
                boolean alterado = false;
                if (!isSenhaComHash(usuario.getSenha())) {
                    usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
                    alterado = true;
                }
                if (usuario.getAtivo() == null) {
                    usuario.setAtivo(true);
                    alterado = true;
                }
                if (!usuario.temStatusPersistido()) {
                    usuario.setStatus(Boolean.FALSE.equals(usuario.getAtivo())
                            ? StatusUsuario.INATIVO : StatusUsuario.ATIVO);
                    alterado = true;
                }
                if (usuario.getTipoUsuario() == TipoUsuario.USUARIO_COMUM) {
                    usuario.setTipoUsuario(TipoUsuario.USUARIO);
                    alterado = true;
                }
                if (alterado) usuarioRepository.save(usuario);
            });
        }

        materialRepository.findAll().forEach(material -> {
            boolean alterado = material.migrarDimensoesLegadas();
            if (material.getAtivo() == null) {
                material.setAtivo(true);
                alterado = true;
            }
            if (alterado) materialRepository.save(material);
        });

        obraRepository.findAll().forEach(obra -> {
            if (obra.getAtivo() == null) {
                obra.setAtivo(true);
                obraRepository.save(obra);
            }
        });
    }

    private boolean isSenhaComHash(String senha) {
        return senha != null && (
                senha.startsWith("$2a$") ||
                        senha.startsWith("$2b$") ||
                        senha.startsWith("$2y$")
        );
    }
}
