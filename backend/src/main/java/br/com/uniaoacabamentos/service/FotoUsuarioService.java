package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;

@Service
public class FotoUsuarioService {
    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final PermissaoService permissaoService;
    private final Path diretorio;

    public FotoUsuarioService(UsuarioRepository usuarioRepository,
                              PermissaoService permissaoService,
                              @Value("${unicontrol.uploads.usuarios-dir:uploads/usuarios}") String diretorio) {
        this.usuarioRepository = usuarioRepository;
        this.permissaoService = permissaoService;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Transactional
    public Usuario salvar(Long usuarioId, MultipartFile arquivo, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        Usuario usuario = buscar(usuarioId);
        byte[] conteudo = lerEValidar(arquivo);
        TipoImagem tipo = validarImagem(conteudo);
        String novoNome = UUID.randomUUID() + tipo.extensao();
        Path destino = resolverSeguro(novoNome);
        String fotoAnterior = usuario.getCaminhoFoto();

        try {
            Files.createDirectories(diretorio);
            Files.write(destino, conteudo, StandardOpenOption.CREATE_NEW);
            usuario.setCaminhoFoto(novoNome);
            Usuario salvo = usuarioRepository.save(usuario);
            apagarSilenciosamente(fotoAnterior);
            return salvo;
        } catch (IOException exception) {
            apagarSilenciosamente(novoNome);
            throw new RuntimeException("Não foi possível armazenar a foto do usuário.");
        }
    }

    @Transactional
    public Usuario remover(Long usuarioId, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        Usuario usuario = buscar(usuarioId);
        String fotoAnterior = usuario.getCaminhoFoto();
        usuario.setCaminhoFoto(null);
        Usuario salvo = usuarioRepository.save(usuario);
        apagarSilenciosamente(fotoAnterior);
        return salvo;
    }

    public FotoArmazenada carregar(Long usuarioId, Usuario usuarioAutenticado) {
        if (!permissaoService.isAdministrador(usuarioAutenticado)
                && !usuarioId.equals(usuarioAutenticado.getId())) {
            throw new RuntimeException("Você não possui permissão para visualizar esta foto.");
        }
        Usuario usuario = buscar(usuarioId);
        if (usuario.getCaminhoFoto() == null || usuario.getCaminhoFoto().isBlank()) {
            throw new RuntimeException("Usuário não possui foto de perfil.");
        }
        try {
            Path arquivo = resolverSeguro(usuario.getCaminhoFoto());
            Resource recurso = new UrlResource(arquivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                throw new RuntimeException("Foto de perfil não encontrada.");
            }
            return new FotoArmazenada(recurso, tipoPorExtensao(usuario.getCaminhoFoto()));
        } catch (IOException exception) {
            throw new RuntimeException("Não foi possível carregar a foto de perfil.");
        }
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    private byte[] lerEValidar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new RuntimeException("Selecione uma imagem.");
        if (arquivo.getSize() > TAMANHO_MAXIMO) throw new RuntimeException("A foto deve possuir no máximo 5 MB.");
        try {
            return arquivo.getBytes();
        } catch (IOException exception) {
            throw new RuntimeException("Não foi possível ler a foto enviada.");
        }
    }

    private TipoImagem identificar(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return TipoImagem.PNG;
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return TipoImagem.JPEG;
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') {
            return TipoImagem.WEBP;
        }
        throw new RuntimeException("O arquivo enviado não é uma imagem JPG, PNG ou WEBP válida.");
    }

    private TipoImagem validarImagem(byte[] bytes) {
        TipoImagem tipo = identificar(bytes);
        if (tipo == TipoImagem.WEBP) {
            validarEstruturaWebp(bytes);
            return tipo;
        }

        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw imagemInvalida();
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0 || (long) largura * altura > 25_000_000L) {
                    throw imagemInvalida();
                }
                BufferedImage imagem = leitor.read(0);
                if (imagem == null) throw imagemInvalida();
            } finally {
                leitor.dispose();
            }
            return tipo;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof RuntimeException runtime
                    && runtime.getMessage() != null && runtime.getMessage().contains("JPG, PNG ou WEBP")) {
                throw runtime;
            }
            throw imagemInvalida();
        }
    }

    private void validarEstruturaWebp(byte[] bytes) {
        if (bytes.length < 20) throw imagemInvalida();
        long tamanhoDeclarado = (bytes[4] & 0xffL) | ((bytes[5] & 0xffL) << 8)
                | ((bytes[6] & 0xffL) << 16) | ((bytes[7] & 0xffL) << 24);
        String bloco = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        long tamanhoBloco = (bytes[16] & 0xffL) | ((bytes[17] & 0xffL) << 8)
                | ((bytes[18] & 0xffL) << 16) | ((bytes[19] & 0xffL) << 24);
        long minimoBloco = bloco.equals("VP8L") ? 5 : 10;
        if (tamanhoDeclarado + 8 > bytes.length
                || !(bloco.equals("VP8 ") || bloco.equals("VP8L") || bloco.equals("VP8X"))
                || tamanhoBloco < minimoBloco || tamanhoBloco > bytes.length - 20L) {
            throw imagemInvalida();
        }
    }

    private RuntimeException imagemInvalida() {
        return new RuntimeException("O arquivo enviado não é uma imagem JPG, PNG ou WEBP válida.");
    }

    private Path resolverSeguro(String nomeArquivo) {
        Path resolvido = diretorio.resolve(nomeArquivo).normalize();
        if (!resolvido.startsWith(diretorio)) throw new RuntimeException("Caminho de foto inválido.");
        return resolvido;
    }

    private void apagarSilenciosamente(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) return;
        try {
            Files.deleteIfExists(resolverSeguro(nomeArquivo));
        } catch (IOException ignored) {
            // A referência do banco continua consistente mesmo se a limpeza física falhar.
        }
    }

    private String tipoPorExtensao(String nome) {
        String minusculo = nome.toLowerCase();
        if (minusculo.endsWith(".png")) return "image/png";
        if (minusculo.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private enum TipoImagem {
        JPEG(".jpg"), PNG(".png"), WEBP(".webp");

        private final String extensao;

        TipoImagem(String extensao) { this.extensao = extensao; }
        public String extensao() { return extensao; }
    }

    public record FotoArmazenada(Resource recurso, String contentType) {}
}
