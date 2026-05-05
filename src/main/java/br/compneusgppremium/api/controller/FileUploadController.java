package br.compneusgppremium.api.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import javax.servlet.http.HttpServletRequest;

@Controller
public class FileUploadController {

    @Autowired
    FilesStorageService storageService;
    private HttpServletRequest request;
//    private static String caminhoImagem = new OperationSystem().placeImageSystem();


    @PostMapping("/api/upload")
    public Object uploadFiles(@RequestParam("title") String title, @RequestParam("files") MultipartFile[] files) {
        String message = "";
        String caminhoImagem = new OperationSystem().placeImageSystem(title);

        try {
            List<String> fileNames = new ArrayList<>();

            Arrays.asList(files).stream().forEach(file -> {

                var ext = FilenameUtils.getExtension(file.getOriginalFilename());
                var fname = UUID.randomUUID().toString();

                if (ext != "")//if ext is there concat
                    fname += "." + ext;

                try {
//                    String realPath = request.getServletContext().getRealPath("");
                    String filename = file.getOriginalFilename(); // Give a random filename here.
                    byte[] bytes = file.getBytes();
//                    Path insPath = Path.of(Paths.get("C:\\Users\\Servo\\Documents\\www\\gppremium\\uploads\\").toString());
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
}
