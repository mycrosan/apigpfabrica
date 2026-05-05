package br.compneusgppremium.api.controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin
@RestController
@RequestMapping("/images")
public class ImageController {
    @PostMapping
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file)
    {
        if (file.isEmpty()){
            throw new RuntimeException("Arquivo não valido!");
        }
        String folder = "E:\\imagens\\";
        try {
            Path pathFolder = Paths.get(folder);
            Files.createDirectories(pathFolder);

            Path pathFile = Paths.get(folder + file.getOriginalFilename());
            Files.write(pathFile, file.getBytes());


        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Tudo ok Picareta Dev");
    return new ResponseEntity(HttpStatus.OK);
    }

}
