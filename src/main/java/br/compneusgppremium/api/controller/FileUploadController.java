package br.compneusgppremium.api.controller;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import br.compneusgppremium.api.message.ResponseMessage;
import br.compneusgppremium.api.service.FilesStorageService;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.OperationSystem;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FileUploadController {

    @Autowired
    FilesStorageService storageService;
    private HttpServletRequest request;
//    private static String caminhoImagem = new OperationSystem().placeImageSystem();


    // Maior lado (em px) das fotos de carcaça depois de comprimidas no servidor —
    // close-up de um campo do pneu não precisa de mais resolução que isso pra IA
    // ou pro operador conferirem depois. Mantém o servidor de encher de fotos
    // grandes agora que o cadastro guiado tira até 15 fotos por carcaça.
    private static final int CARCACA_MAX_LADO_PX = 1280;
    private static final float CARCACA_QUALIDADE_JPEG = 0.7f;

    @PostMapping("/api/upload")
    public Object uploadFiles(@RequestParam("title") String title, @RequestParam("files") MultipartFile[] files) {
        String message = "";
        String caminhoImagem = new OperationSystem().placeImageSystem(title);
        boolean comprimir = "carcaca".equals(title);

        try {
            List<String> fileNames = new ArrayList<>();

            Arrays.asList(files).stream().forEach(file -> {

                var ext = FilenameUtils.getExtension(file.getOriginalFilename());
                var fname = UUID.randomUUID().toString();

                try {
                    byte[] bytes = file.getBytes();
                    if (comprimir) {
                        byte[] comprimido = comprimirImagem(bytes);
                        if (comprimido != null) {
                            bytes = comprimido;
                            ext = "jpg"; // sempre reencodada como JPEG quando a compressão funciona
                        }
                    }
                    if (ext != null && !ext.isEmpty()) {
                        fname += "." + ext;
                    }
                    String insPathN = caminhoImagem + fname;
                    Files.write(Paths.get(insPathN), bytes);
                    fileNames.add(fname);
                } catch (IOException e) {
                    System.out.println(e);
                }
            });

            message = "Uploaded the files successfully: ";


//            ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY, message , null,"Erro");
//            return apiError;

            var response = ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message, fileNames));
            System.out.println(response);
            return response;

        } catch (Exception e) {
            message = "Falha ao fazer o upload do arquivo!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message, null));
        }
    }


//	@GetMapping("/files")
//	public ResponseEntity<List<FileInfo>> getListFiles() {
//		List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
//			String filename = path.getFileName().toString();
//			String url = MvcUriComponentsBuilder
//					.fromMethodName(FilesController.class, "getFile", path.getFileName().toString()).build().toString();
//
//			return new FileInfo(filename, url);
//		}).collect(Collectors.toList());
//
//		return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
//	}

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = storageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    private Object renameValueToUid() {
        return null;
    }

    // Redimensiona (maior lado <= CARCACA_MAX_LADO_PX) e reencoda como JPEG com
    // qualidade reduzida, pra não pesar o disco do servidor. Retorna null se a
    // imagem não puder ser decodificada (formato não suportado/corrompida) — nesse
    // caso o chamador grava os bytes originais, sem quebrar o upload.
    private byte[] comprimirImagem(byte[] original) {
        try {
            BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(original));
            if (imagem == null) {
                return null;
            }

            int largura = imagem.getWidth();
            int altura = imagem.getHeight();
            int maiorLado = Math.max(largura, altura);
            BufferedImage redimensionada = imagem;
            if (maiorLado > CARCACA_MAX_LADO_PX) {
                double escala = CARCACA_MAX_LADO_PX / (double) maiorLado;
                int novaLargura = Math.max(1, (int) Math.round(largura * escala));
                int novaAltura = Math.max(1, (int) Math.round(altura * escala));
                Image escalada = imagem.getScaledInstance(novaLargura, novaAltura, Image.SCALE_SMOOTH);
                redimensionada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
                redimensionada.getGraphics().drawImage(escalada, 0, 0, null);
            } else if (imagem.getType() != BufferedImage.TYPE_INT_RGB) {
                // JPEG não suporta transparência — garante fundo opaco ao reencodar PNGs/etc.
                BufferedImage semAlfa = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
                semAlfa.getGraphics().drawImage(imagem, 0, 0, null);
                redimensionada = semAlfa;
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam parametros = writer.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CARCACA_QUALIDADE_JPEG);

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            writer.setOutput(new MemoryCacheImageOutputStream(saida));
            writer.write(null, new IIOImage(redimensionada, null, null), parametros);
            writer.dispose();

            return saida.toByteArray();
        } catch (Exception ex) {
            System.out.println("Falha ao comprimir imagem, gravando original: " + ex);
            return null;
        }
    }
}
