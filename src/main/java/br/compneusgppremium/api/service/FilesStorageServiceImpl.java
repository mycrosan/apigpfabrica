package br.compneusgppremium.api.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private final Path root_p = Paths.get("/opt/wildfly/standalone/deployments/uploads");
    private final Path root_cred_p = Paths.get("/opt/wildfly/standalone/deployments/uploads/credenciados");

    private final Path root = Paths.get("uploads");
    private final Path root_cred = Paths.get("uploads/credenciados");

    @Override
    public void init() {
        System.out.println("Inicializando" + root + root_cred);
        String filePath = new File("").getPath();
        System.out.println("pasta" + filePath);
        if (filePath == "/") {
            this.createDir(root_p);
            this.createDir(root_cred_p);
        } else {
            this.createDir(root);
            this.createDir(root_cred);
        }
    }

    @Override
    public void save(MultipartFile file) {
        try {
            Files.copy(file.getInputStream(), this.root.resolve(file.getOriginalFilename()));
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(root.toFile());
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }

    private void createDir(Path path_place) {
        try {
            if (!Files.exists(path_place)) {
                Files.createDirectory(path_place);
            }
        } catch (IOException e) {
            throw new RuntimeException("Problema ao criar a pasta upload! " + path_place);
        }
    }

}
