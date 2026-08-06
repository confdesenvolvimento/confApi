package com.confApi.chatconfianca.service;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Armazena anexos do chat no Object Storage privado da OCI. */
@Service
public class OciObjectStorageService {
    private static final String DEFAULT_NAMESPACE = "grxzso80vgqb";
    private static final String DEFAULT_BUCKET = "Imagens_upload";
    private static final String DEFAULT_REGION = "sa-saopaulo-1";
    private static final String DEFAULT_PREFIX = "chat-confianca";

    private final String namespace;
    private final String bucket;
    private final String prefix;
    private final Region region;
    private volatile ObjectStorageClient client;

    public OciObjectStorageService() {
        this.namespace = configuracao("OCI_OBJECT_STORAGE_NAMESPACE", DEFAULT_NAMESPACE);
        this.bucket = configuracao("OCI_OBJECT_STORAGE_BUCKET", DEFAULT_BUCKET);
        this.prefix = configuracao("OCI_OBJECT_STORAGE_PREFIX", DEFAULT_PREFIX);
        this.region = Region.fromRegionId(configuracao("OCI_OBJECT_STORAGE_REGION", DEFAULT_REGION));
    }

    public String novoObjectKey(Long conversaId, String nomeArmazenado) {
        String nome = Objects.requireNonNull(nomeArmazenado, "nomeArmazenado");
        return prefix + "/anexos/" + Objects.requireNonNull(conversaId, "conversaId") + "/" + nome;
    }

    public void enviar(String objectKey, String mimeType, byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("Conteudo do anexo vazio.");
        }
        PutObjectRequest request = PutObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectKey)
                .contentType(mimeType)
                .contentLength((long) conteudo.length)
                .putObjectBody(new ByteArrayInputStream(conteudo))
                .build();
        cliente().putObject(request);
    }

    public byte[] baixar(String objectKey) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .namespaceName(namespace)
                .bucketName(bucket)
                .objectName(objectKey)
                .build();
        try (InputStream input = cliente().getObject(request).getInputStream()) {
            return input.readAllBytes();
        }
    }

    public void remover(String objectKey) {
        try {
            cliente().deleteObject(
                    com.oracle.bmc.objectstorage.requests.DeleteObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucket)
                            .objectName(objectKey)
                            .build());
        } catch (RuntimeException ignored) {
            // A limpeza e compensatoria e nao deve mascarar o erro original.
        }
    }

    private ObjectStorageClient cliente() {
        ObjectStorageClient atual = client;
        if (atual == null) {
            synchronized (this) {
                atual = client;
                if (atual == null) {
                    atual = ObjectStorageClient.builder()
                            .region(region)
                            .build(InstancePrincipalsAuthenticationDetailsProvider.builder().build());
                    client = atual;
                }
            }
        }
        return atual;
    }

    private String configuracao(String nome, String padrao) {
        String valor = System.getProperty(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(nome);
        }
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    @PreDestroy
    public void fechar() {
        ObjectStorageClient atual = client;
        if (atual != null) {
            atual.close();
        }
    }
}
