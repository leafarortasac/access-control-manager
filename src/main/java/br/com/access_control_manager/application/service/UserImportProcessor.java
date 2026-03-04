package br.com.access_control_manager.application.service;

import br.com.access_control_manager.application.dto.UserImportCsvDTO;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserImportProcessor {

    @Async
    public void processCsvAsync(byte[] csvContent, UUID tenantId, Consumer<UserImportCsvDTO> rowProcessor) {
        log.info("[Async] Iniciando processamento de {} bytes para o tenant {}", csvContent.length, tenantId);

        try (Reader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvContent), StandardCharsets.UTF_8))) {

            CsvToBean<UserImportCsvDTO> csvToBean = new CsvToBeanBuilder<UserImportCsvDTO>(reader)
                    .withType(UserImportCsvDTO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withSeparator(';')
                    .build();

            List<UserImportCsvDTO> rows = csvToBean.parse();
            log.info("[Async] Encontradas {} linhas para processar.", rows.size());

            rows.forEach(rowProcessor);

            log.info("[Async] Importação finalizada para o tenant {}", tenantId);
        } catch (Exception e) {
            log.error("[Async] Erro crítico no processamento do CSV", e);
        }
    }
}