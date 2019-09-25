package com.example.filedemo.controller;

import com.example.filedemo.payload.UploadFileResponse;
import com.example.filedemo.service.FileStorageService;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
    }

    @PostMapping("/uploadMultipleFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(file))
                .collect(Collectors.toList());
    }

    @CrossOrigin(origins="http://localhost:8000")
    @GetMapping("/downloadFile/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch(IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determine
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @RequestMapping("/hello")
    public RedirectView hello() {
        return new RedirectView("http://localhost:8000");
    }

    @CrossOrigin(origins="http://localhost:8000")
    @GetMapping("/configJson/{index}")
    public Map<String,Object> configJson(@PathVariable String index) {

//        {
//            "embeddings": [
//            {
//                "metadataPath": "http://localhost:8080/downloadFile/words_meta.tsv",
//                    "tensorName": "words",
//                    "tensorPath": "http://localhost:8080/downloadFile/words_data.tsv",
//                    "tensorShape": [
//                500,
//                        48
//            ]
//            }
//    ],
//            "modelCheckpointPath": "Demo datasets"
//        }
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> infoMap = new HashMap<>();
        infoMap.put("metadataPath", "http://localhost:8080/downloadFile/words_meta.tsv");
        infoMap.put("tensorName", "hello-world");
        infoMap.put("tensorPath", "http://localhost:8080/downloadFile/words_data.tsv");
        List<Integer> shape = new ArrayList<>();
        shape.add(500);
        shape.add(48);
        infoMap.put("tensorShape", shape);

        List<Object> obj = new ArrayList<>();
        obj.add(infoMap);
        map.put("embeddings", obj);
        map.put("modelCheckpointPath", "Demo datasets");

        System.out.println(map.toString());

        return map;
    }
}
