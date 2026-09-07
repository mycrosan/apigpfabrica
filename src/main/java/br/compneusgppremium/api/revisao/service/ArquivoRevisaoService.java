package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.leitura.service.ArquivosLeituraService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
/** Volume separado: reconciliação de sessões não remove evidências importadas. */
@Service
public class ArquivoRevisaoService {
    private final ArquivosLeituraService arquivos;
    public ArquivoRevisaoService(@Value("${pneus.revisao.diretorio:./uploads/revisao}") final String diretorio) {
        arquivos = new ArquivosLeituraService(diretorio);
    }
    public String salvar(final byte[] bytes) { return arquivos.salvar(bytes); }
    public byte[] ler(final String nome) { return arquivos.ler(nome); }
}
