package br.compneusgppremium.api.repository;
import br.compneusgppremium.api.util.ResumoEntidade;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ResumoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ResumoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Long> getResumo(ResumoEntidade entidade) {
        String sql = String.format(
                "SELECT\n" +
                        "    SUM(CASE WHEN DATE(%1$s) = CURDATE() THEN 1 ELSE 0 END) AS hoje,\n" +
                        "    SUM(CASE WHEN DATE(%1$s) = CURDATE() - INTERVAL 1 DAY THEN 1 ELSE 0 END) AS ontem,\n" +
                        "    SUM(CASE WHEN DATE(%1$s) = CURDATE() - INTERVAL 2 DAY THEN 1 ELSE 0 END) AS anteontem,\n" +
                        "    SUM(CASE WHEN YEAR(%1$s) = YEAR(CURDATE()) AND MONTH(%1$s) = MONTH(CURDATE()) THEN 1 ELSE 0 END) AS mesAtual,\n" +
                        "    SUM(CASE WHEN YEAR(%1$s) = YEAR(CURDATE() - INTERVAL 1 MONTH) AND MONTH(%1$s) = MONTH(CURDATE() - INTERVAL 1 MONTH) THEN 1 ELSE 0 END) AS mesPassado,\n" +
                        "    SUM(CASE WHEN YEAR(%1$s) = YEAR(CURDATE() - INTERVAL 2 MONTH) AND MONTH(%1$s) = MONTH(CURDATE() - INTERVAL 2 MONTH) THEN 1 ELSE 0 END) AS mesRetrasado\n" +
                        "FROM %2$s",
                entidade.getColuna(),
                entidade.getTabela()
        );

        return jdbcTemplate.queryForObject(sql, Map.of(), (rs, rowNum) -> Map.of(
                "hoje", rs.getLong("hoje"),
                "ontem", rs.getLong("ontem"),
                "anteontem", rs.getLong("anteontem"),
                "mesAtual", rs.getLong("mesAtual"),
                "mesPassado", rs.getLong("mesPassado"),
                "mesRetrasado", rs.getLong("mesRetrasado")
        ));
    }
}
