package br.com.uniaoacabamentos.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Ajustes pequenos de compatibilidade para bancos criados por versões anteriores.
 * Não remove registros nem recria tabelas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseCompatibilityMigration implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseCompatibilityMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        if (!isPostgreSql()) return;

        jdbcTemplate.execute("""
                ALTER TABLE usuario
                    DROP CONSTRAINT IF EXISTS usuario_tipo_usuario_check,
                    ADD CONSTRAINT usuario_tipo_usuario_check
                        CHECK (tipo_usuario IN ('ADMINISTRADOR', 'USUARIO', 'USUARIO_COMUM'))
                """);

        atualizarRestricoesDeEnums();
        migrarColunaValorMaoDeObra();
        migrarColunaUsuarioResponsavel();
        permitirOrcamentoDeObraSemObraCadastrada();
    }

    private void atualizarRestricoesDeEnums() {
        // O Hibernate não atualiza CHECK constraints criadas por versões antigas dos enums.
        // Primeiro valida os dados legados para nunca apagar ou converter valores silenciosamente.
        List<String> tiposOrcamentoInvalidos = jdbcTemplate.queryForList("""
                SELECT DISTINCT tipo_orcamento
                  FROM orcamento
                 WHERE tipo_orcamento IS NOT NULL
                   AND tipo_orcamento NOT IN ('OBRA', 'MONTADOR')
                """, String.class);
        List<String> statusOrcamentoInvalidos = jdbcTemplate.queryForList("""
                SELECT DISTINCT status
                  FROM orcamento
                 WHERE status IS NOT NULL
                   AND status NOT IN ('CALCULADO', 'CONTRATADO', 'NAO_CONTRATADO')
                """, String.class);
        List<String> statusObraInvalidos = jdbcTemplate.queryForList("""
                SELECT DISTINCT status
                  FROM obra
                 WHERE status IS NOT NULL
                   AND status NOT IN ('CADASTRADA', 'EM_ANDAMENTO', 'PAUSADA', 'FINALIZADA', 'CANCELADA')
                """, String.class);

        impedirMigracaoComValoresDesconhecidos("tipo de orçamento", tiposOrcamentoInvalidos);
        impedirMigracaoComValoresDesconhecidos("status de orçamento", statusOrcamentoInvalidos);
        impedirMigracaoComValoresDesconhecidos("status de obra", statusObraInvalidos);

        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    DROP CONSTRAINT IF EXISTS orcamento_tipo_orcamento_check,
                    ADD CONSTRAINT orcamento_tipo_orcamento_check
                        CHECK (tipo_orcamento IN ('OBRA', 'MONTADOR'))
                """);

        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    DROP CONSTRAINT IF EXISTS orcamento_status_check,
                    ADD CONSTRAINT orcamento_status_check
                        CHECK (status IN ('CALCULADO', 'CONTRATADO', 'NAO_CONTRATADO'))
                """);

        jdbcTemplate.execute("""
                ALTER TABLE obra
                    DROP CONSTRAINT IF EXISTS obra_status_check,
                    ADD CONSTRAINT obra_status_check
                        CHECK (status IN ('CADASTRADA', 'EM_ANDAMENTO', 'PAUSADA', 'FINALIZADA', 'CANCELADA'))
                """);
    }

    private void impedirMigracaoComValoresDesconhecidos(String campo, List<String> valores) {
        if (valores.isEmpty()) return;
        throw new IllegalStateException(
                "A migração foi interrompida porque há " + campo
                        + " com valor legado desconhecido: " + String.join(", ", valores) + ".");
    }

    private void migrarColunaUsuarioResponsavel() {
        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    ADD COLUMN IF NOT EXISTS usuario_criador_id bigint
                """);

        if (colunaExiste("orcamento", "usuario_id")) {
            jdbcTemplate.execute("""
                    UPDATE orcamento
                       SET usuario_criador_id = COALESCE(usuario_criador_id, usuario_id)
                     WHERE usuario_criador_id IS NULL
                    """);
            // Mantém a coluna antiga para compatibilidade, sem bloquear os novos registros.
            jdbcTemplate.execute("""
                    ALTER TABLE orcamento
                        ALTER COLUMN usuario_id DROP NOT NULL
                    """);
        }

        Integer semResponsavel = jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM orcamento
                 WHERE usuario_criador_id IS NULL
                """, Integer.class);
        if (semResponsavel != null && semResponsavel > 0) {
            throw new IllegalStateException(
                    "Existem orçamentos antigos sem usuário responsável; a migração foi interrompida para preservar a integridade dos dados.");
        }

        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    ALTER COLUMN usuario_criador_id SET NOT NULL
                """);
    }

    private void permitirOrcamentoDeObraSemObraCadastrada() {
        if (!colunaExiste("orcamento", "obra_id")) return;

        // O orçamento de OBRA nasce antes da obra e só recebe este vínculo na contratação.
        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    ALTER COLUMN obra_id DROP NOT NULL
                """);
    }

    private void migrarColunaValorMaoDeObra() {
        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    ADD COLUMN IF NOT EXISTS valor_mao_obra numeric(19, 2)
                """);

        if (colunaExiste("orcamento", "valor_mao_de_obra")) {
            jdbcTemplate.execute("""
                    UPDATE orcamento
                       SET valor_mao_obra = COALESCE(valor_mao_obra, valor_mao_de_obra, 0)
                     WHERE valor_mao_obra IS NULL
                    """);
            // A coluna criada pela versão intermediária deixa de ser usada, mas é preservada.
            jdbcTemplate.execute("""
                    ALTER TABLE orcamento
                        ALTER COLUMN valor_mao_de_obra DROP NOT NULL
                    """);
        } else {
            jdbcTemplate.execute("""
                    UPDATE orcamento
                       SET valor_mao_obra = 0
                     WHERE valor_mao_obra IS NULL
                    """);
        }

        jdbcTemplate.execute("""
                ALTER TABLE orcamento
                    ALTER COLUMN valor_mao_obra SET DEFAULT 0,
                    ALTER COLUMN valor_mao_obra SET NOT NULL
                """);
    }

    private boolean colunaExiste(String tabela, String coluna) {
        Boolean existe = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM information_schema.columns
                     WHERE table_schema = current_schema()
                       AND table_name = ?
                       AND column_name = ?
                )
                """, Boolean.class, tabela, coluna);
        return Boolean.TRUE.equals(existe);
    }

    private boolean isPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return "PostgreSQL".equalsIgnoreCase(
                    connection.getMetaData().getDatabaseProductName());
        }
    }
}
