package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.ProducaoModel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
public class DownloadsController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping(path = "/api/download")
    public ResponseEntity<ByteArrayResource> downloadProducao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date dataFim) {
        
        StringBuilder sqlBuilder = new StringBuilder("SELECT p FROM producao p");
        
        // Adiciona filtros de data se fornecidos
        if (dataInicio != null || dataFim != null) {
            sqlBuilder.append(" WHERE ");
            
            if (dataInicio != null) {
                sqlBuilder.append("p.dt_create >= :dataInicio");
                
                if (dataFim != null) {
                    sqlBuilder.append(" AND ");
                }
            }
            
            if (dataFim != null) {
                sqlBuilder.append("p.dt_create <= :dataFim");
            }
        }
        
        sqlBuilder.append(" ORDER BY p.dt_create DESC");
        
        Query query = entityManager.createQuery(sqlBuilder.toString());
        
        // Define os parâmetros da consulta
        if (dataInicio != null) {
            query.setParameter("dataInicio", dataInicio);
        }
        
        if (dataFim != null) {
            query.setParameter("dataFim", dataFim);
        }
        
        // Limita o número de resultados
        query.setMaxResults(500);
        
        List<?> results = query.getResultList();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("producao");
            Row headerRow = sheet.createRow(0);

            // Definindo os cabeçalhos das colunas
            String[] columnNames = {
                    "ID",
                    "Número Etiqueta",
                    "Data Produção",
                    "Data Carcaça",
                    "Medida",
                    "Marca",
                    "Modelo",
                    "Matriz",
                    "Regra Utilizada",
                    "Tamanho Raspado",
                    "Camelback Utilizado",
                    "Antiquebra 1",
                    "Antiquebra 2",
                    "Antiquebra 3",
                    "Espessuramento",
                    "Tempo"};

            for (int i = 0; i < columnNames.length; i++) {
                headerRow.createCell(i).setCellValue(columnNames[i]);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            int rowNum = 1;
            for (Object result : results) {
                ProducaoModel producao = (ProducaoModel) result;
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(producao.getId());
                row.createCell(1).setCellValue(producao.getCarcaca().getNumero_etiqueta());
                row.createCell(2).setCellValue(dateFormat.format(producao.getDt_create()));
                row.createCell(3).setCellValue(dateFormat.format(producao.getCarcaca().getDt_create()));
                row.createCell(4).setCellValue(producao.getCarcaca().getMedida().getDescricao());
                row.createCell(5).setCellValue(producao.getCarcaca().getModelo().getMarca().getDescricao()); // Supondo que ModeloModel tenha um campo 'nome'
                row.createCell(6).setCellValue(producao.getCarcaca().getModelo().getDescricao()); // Supondo que MedidaModel tenha um campo 'descricao'
                row.createCell(7).setCellValue(producao.getRegra().getMatriz().getDescricao());
                row.createCell(8).setCellValue(producao.getMedida_pneu_raspado());
                row.createCell(9).setCellValue(producao.getRegra().getCamelback().getDescricao());
                row.createCell(10).setCellValue(producao.getRegra().getAntiquebra1().getDescricao());
                row.createCell(11).setCellValue(producao.getRegra().getAntiquebra2().getDescricao());
                row.createCell(12).setCellValue(producao.getRegra().getAntiquebra3().getDescricao());
                row.createCell(13).setCellValue(producao.getRegra().getEspessuramento().getDescricao());
                row.createCell(14).setCellValue(producao.getRegra().getTempo());
            }

            workbook.write(out);
            
            // Cria um nome de arquivo que inclui o período de datas, se fornecido
            SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyyMMdd");
            String fileName = "Rel_Producao";
            
            if (dataInicio != null && dataFim != null) {
                fileName += "_" + fileNameDateFormat.format(dataInicio) + "_a_" + fileNameDateFormat.format(dataFim);
            } else if (dataInicio != null) {
                fileName += "_desde_" + fileNameDateFormat.format(dataInicio);
            } else if (dataFim != null) {
                fileName += "_ate_" + fileNameDateFormat.format(dataFim);
            }
            
            fileName += ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(out.toByteArray()));
        } catch (Exception e) {
            // Log the exception
            System.err.println("Erro ao gerar relatório: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
