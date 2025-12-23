package com.cloudstorage.fx.components;

import com.cloudstorage.config.MinioConfig;
import com.cloudstorage.config.SessionManager;
import com.cloudstorage.fx.utils.AlertUtils;
import com.cloudstorage.fx.utils.FileUtils;
import com.cloudstorage.model.User;
import io.minio.MinioClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Dialogue unifié pour la prévisualisation de tous types de médias.
 * Supporte : Images, PDF, Vidéos, Audio et autres documents.
 */
public class MediaPreviewDialog {

    private final Stage dialogStage;
    private final BorderPane root;
    private final MediaPreviewService previewService;
    private MediaPlayer mediaPlayer;

    // Informations du fichier
    private final String fileName;
    private final String bucketName;

    // Constantes de style
    private static final String DIALOG_BG = "#1a1a2e";
    private static final String HEADER_BG = "#16213e";
    private static final String CONTROLS_BG = "#0f3460";
    private static final String ACCENT_COLOR = "#e94560";

    public MediaPreviewDialog(String fileName, String bucketName, MinioClient minioClient) {
        this.fileName = fileName;
        this.bucketName = bucketName;
        this.previewService = new MediaPreviewService(minioClient, bucketName);

        // Configuration du stage
        this.dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.UNDECORATED);
        dialogStage.setTitle("Aperçu - " + fileName);

        // Conteneur principal
        this.root = new BorderPane();
        root.setStyle("-fx-background-color: " + DIALOG_BG + "; -fx-background-radius: 15;");

        // Header avec titre et bouton fermer
        HBox header = createHeader();
        root.setTop(header);

        // Charger le contenu selon le type
        loadContent();

        // Configuration de la scène
        Scene scene = new Scene(root, 900, 650);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setScene(scene);

        // Fermer avec ESC
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                close();
            }
        });
    }

    /**
     * Crée l'en-tête du dialogue
     */
    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: " + HEADER_BG + "; -fx-background-radius: 15 15 0 0;");

        // Icône du type de fichier
        FontIcon fileIcon = getFileIcon();
        fileIcon.setIconSize(24);
        fileIcon.setIconColor(Color.WHITE);

        // Nom du fichier
        Label titleLabel = new Label(FileUtils.truncateFileName(fileName, 50));
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Bouton fermer
        Button closeBtn = new Button();
        FontIcon closeIcon = new FontIcon(FontAwesomeSolid.TIMES);
        closeIcon.setIconSize(18);
        closeIcon.setIconColor(Color.WHITE);
        closeBtn.setGraphic(closeIcon);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-background-radius: 5; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));

        header.getChildren().addAll(fileIcon, titleLabel, closeBtn);
        return header;
    }

    /**
     * Charge le contenu selon le type de média
     */
    private void loadContent() {
        MediaPreviewService.MediaType type = MediaPreviewService.detectMediaType(fileName);

        switch (type) {
            case IMAGE -> loadImagePreview();
            case VIDEO -> loadVideoPlayer();
            case AUDIO -> loadAudioPlayer();
            case PDF -> loadPdfPreview();
            default -> loadGenericPreview(type);
        }
    }

    /**
     * Affiche une image
     */
    private void loadImagePreview() {
        StackPane container = createLoadingContainer("Chargement de l'image...");
        root.setCenter(container);

        Task<Image> task = previewService.loadImageAsync(fileName);

        task.setOnSucceeded(e -> {
            ImageView imageView = new ImageView(task.getValue());
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(850);
            imageView.setFitHeight(550);

            StackPane imageContainer = new StackPane(imageView);
            imageContainer.setStyle("-fx-background-color: #0d0d0d;");
            imageContainer.setPadding(new Insets(20));

            root.setCenter(imageContainer);
        });

        task.setOnFailed(e -> showError("Impossible de charger l'image", task.getException().getMessage()));

        new Thread(task).start();
    }

    /**
     * Lecteur vidéo intégré
     */
    private void loadVideoPlayer() {
        StackPane container = createLoadingContainer("Préparation de la vidéo...");
        root.setCenter(container);

        new Thread(() -> {
            try {
                String streamUrl = previewService.getStreamingUrl(fileName);

                Platform.runLater(() -> {
                    try {
                        Media media = new Media(streamUrl);
                        mediaPlayer = new MediaPlayer(media);

                        MediaView mediaView = new MediaView(mediaPlayer);
                        mediaView.setPreserveRatio(true);
                        mediaView.setFitWidth(850);
                        mediaView.setFitHeight(480);

                        VBox playerContainer = new VBox(10);
                        playerContainer.setAlignment(Pos.CENTER);
                        playerContainer.setStyle("-fx-background-color: black;");

                        // Contrôles vidéo
                        HBox controls = createVideoControls();
                        
                        playerContainer.getChildren().addAll(mediaView, controls);
                        root.setCenter(playerContainer);

                        // Auto-play
                        mediaPlayer.play();

                    } catch (Exception ex) {
                        showError("Erreur vidéo", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Impossible de charger la vidéo", ex.getMessage()));
            }
        }).start();
    }

    /**
     * Crée les contrôles pour le lecteur vidéo/audio
     */
    private HBox createVideoControls() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15));
        controls.setStyle("-fx-background-color: " + CONTROLS_BG + ";");

        // Bouton Play/Pause
        Button playPauseBtn = new Button();
        FontIcon playIcon = new FontIcon(FontAwesomeSolid.PLAY);
        playIcon.setIconSize(18);
        playIcon.setIconColor(Color.WHITE);
        playPauseBtn.setGraphic(playIcon);
        playPauseBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");

        playPauseBtn.setOnAction(e -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                ((FontIcon) playPauseBtn.getGraphic()).setIconCode(FontAwesomeSolid.PLAY);
            } else {
                mediaPlayer.play();
                ((FontIcon) playPauseBtn.getGraphic()).setIconCode(FontAwesomeSolid.PAUSE);
            }
        });

        // Bouton Stop
        Button stopBtn = createControlButton(FontAwesomeSolid.STOP, () -> {
            mediaPlayer.stop();
            ((FontIcon) playPauseBtn.getGraphic()).setIconCode(FontAwesomeSolid.PLAY);
        });

        // Slider de progression
        Slider progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(400);
        progressSlider.setStyle("-fx-control-inner-background: #2d3436;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!progressSlider.isValueChanging() && mediaPlayer.getTotalDuration() != null) {
                double progress = newVal.toSeconds() / mediaPlayer.getTotalDuration().toSeconds() * 100;
                progressSlider.setValue(progress);
            }
        });

        progressSlider.setOnMousePressed(e -> mediaPlayer.pause());
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() / 100 * mediaPlayer.getTotalDuration().toSeconds();
                mediaPlayer.seek(Duration.seconds(seekTime));
                mediaPlayer.play();
            }
        });

        // Label de temps
        Label timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            String current = formatTime(newVal);
            String total = mediaPlayer.getTotalDuration() != null ? formatTime(mediaPlayer.getTotalDuration()) : "00:00";
            timeLabel.setText(current + " / " + total);
        });

        // Contrôle du volume
        FontIcon volumeIcon = new FontIcon(FontAwesomeSolid.VOLUME_UP);
        volumeIcon.setIconSize(16);
        volumeIcon.setIconColor(Color.WHITE);

        Slider volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(80);
        mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty());

        controls.getChildren().addAll(playPauseBtn, stopBtn, progressSlider, timeLabel, volumeIcon, volumeSlider);
        return controls;
    }

    /**
     * Lecteur audio
     */
    private void loadAudioPlayer() {
        VBox container = new VBox(30);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(50));
        container.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Icône audio grande
        FontIcon audioIcon = new FontIcon(FontAwesomeSolid.MUSIC);
        audioIcon.setIconSize(120);
        audioIcon.setIconColor(Color.web(ACCENT_COLOR));

        // Nom du fichier
        Label nameLabel = new Label(fileName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Indicateur de chargement
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);

        container.getChildren().addAll(audioIcon, nameLabel, spinner);
        root.setCenter(container);

        new Thread(() -> {
            try {
                String streamUrl = previewService.getStreamingUrl(fileName);

                Platform.runLater(() -> {
                    try {
                        Media media = new Media(streamUrl);
                        mediaPlayer = new MediaPlayer(media);

                        container.getChildren().remove(spinner);

                        HBox controls = createVideoControls();
                        controls.setMaxWidth(600);
                        container.getChildren().add(controls);

                        mediaPlayer.play();

                    } catch (Exception ex) {
                        showError("Erreur audio", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Impossible de charger l'audio", ex.getMessage()));
            }
        }).start();
    }

    /**
     * Aperçu PDF (première page)
     */
    private void loadPdfPreview() {
        StackPane container = createLoadingContainer("Rendu du PDF...");
        root.setCenter(container);

        Task<Image> task = previewService.generatePdfPreviewAsync(fileName);

        task.setOnSucceeded(e -> {
            ImageView imageView = new ImageView(task.getValue());
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(750);
            imageView.setFitHeight(550);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: #2d3436; -fx-background-color: #2d3436;");

            VBox pdfContainer = new VBox(10);
            pdfContainer.setAlignment(Pos.CENTER);
            pdfContainer.setPadding(new Insets(10));

            Label infoLabel = new Label("Aperçu de la première page - Double-cliquez pour ouvrir");
            infoLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 12px;");

            pdfContainer.getChildren().addAll(scrollPane, infoLabel);
            root.setCenter(pdfContainer);

            // Double-clic pour ouvrir dans le lecteur externe
            scrollPane.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    openInExternalApp();
                }
            });
        });

        task.setOnFailed(e -> showError("Impossible de charger le PDF", task.getException().getMessage()));

        new Thread(task).start();
    }

    /**
     * Aperçu générique pour les fichiers non supportés
     */
    private void loadGenericPreview(MediaPreviewService.MediaType type) {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(50));
        container.setStyle("-fx-background-color: " + DIALOG_BG + ";");

        FontIcon icon = getFileIcon();
        icon.setIconSize(100);

        Label typeLabel = new Label(getTypeDescription(type));
        typeLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 14px;");

        Label sizeLabel = new Label("Aperçu non disponible pour ce type de fichier");
        sizeLabel.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");

        Button openBtn = new Button("Ouvrir avec l'application par défaut");
        openBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");
        openBtn.setOnAction(e -> openInExternalApp());

        container.getChildren().addAll(icon, typeLabel, sizeLabel, openBtn);
        root.setCenter(container);
    }

    /**
     * Crée un conteneur avec indicateur de chargement
     */
    private StackPane createLoadingContainer(String message) {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #0d0d0d;");

        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(50, 50);

        Label loadingLabel = new Label(message);
        loadingLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 14px;");

        loadingBox.getChildren().addAll(spinner, loadingLabel);
        container.getChildren().add(loadingBox);

        return container;
    }

    /**
     * Affiche un message d'erreur
     */
    private void showError(String title, String message) {
        VBox errorBox = new VBox(15);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(50));

        FontIcon errorIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        errorIcon.setIconSize(60);
        errorIcon.setIconColor(Color.web(ACCENT_COLOR));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 12px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(400);

        errorBox.getChildren().addAll(errorIcon, titleLabel, msgLabel);
        root.setCenter(errorBox);
    }

    /**
     * Crée un bouton de contrôle
     */
    private Button createControlButton(FontAwesomeSolid iconCode, Runnable action) {
        Button btn = new Button();
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(14);
        icon.setIconColor(Color.WHITE);
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 5; -fx-min-width: 35; -fx-min-height: 35; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * Obtient l'icône correspondant au type de fichier
     */
    private FontIcon getFileIcon() {
        String ext = FileUtils.getFileExtension(fileName);
        MediaPreviewService.MediaType type = MediaPreviewService.detectMediaType(fileName);

        FontAwesomeSolid iconCode;
        String color;

        switch (type) {
            case IMAGE -> { iconCode = FontAwesomeSolid.IMAGE; color = "#9b59b6"; }
            case VIDEO -> { iconCode = FontAwesomeSolid.VIDEO; color = "#e74c3c"; }
            case AUDIO -> { iconCode = FontAwesomeSolid.MUSIC; color = "#3498db"; }
            case PDF -> { iconCode = FontAwesomeSolid.FILE_PDF; color = "#e74c3c"; }
            case DOCUMENT -> {
                if (ext.equals("doc") || ext.equals("docx")) {
                    iconCode = FontAwesomeSolid.FILE_WORD; color = "#2980b9";
                } else if (ext.equals("xls") || ext.equals("xlsx")) {
                    iconCode = FontAwesomeSolid.FILE_EXCEL; color = "#27ae60";
                } else if (ext.equals("ppt") || ext.equals("pptx")) {
                    iconCode = FontAwesomeSolid.FILE_POWERPOINT; color = "#e67e22";
                } else {
                    iconCode = FontAwesomeSolid.FILE_ALT; color = "#7f8c8d";
                }
            }
            case ARCHIVE -> { iconCode = FontAwesomeSolid.FILE_ARCHIVE; color = "#f39c12"; }
            default -> { iconCode = FontAwesomeSolid.FILE; color = "#7f8c8d"; }
        }

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconColor(Color.web(color));
        return icon;
    }

    /**
     * Obtient la description du type de fichier
     */
    private String getTypeDescription(MediaPreviewService.MediaType type) {
        return switch (type) {
            case IMAGE -> "Fichier Image";
            case VIDEO -> "Fichier Vidéo";
            case AUDIO -> "Fichier Audio";
            case PDF -> "Document PDF";
            case DOCUMENT -> "Document";
            case ARCHIVE -> "Archive";
            default -> "Fichier";
        };
    }

    /**
     * Formate la durée en mm:ss
     */
    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.toSeconds() % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Ouvre le fichier avec l'application par défaut du système
     */
    private void openInExternalApp() {
        new Thread(() -> {
            try {
                String url = previewService.getPresignedUrl(fileName, 1);
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showError("Erreur", "Impossible d'ouvrir le fichier: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Affiche le dialogue
     */
    public void show() {
        dialogStage.showAndWait();
    }

    /**
     * Ferme le dialogue et libère les ressources
     */
    public void close() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        dialogStage.close();
    }

    /**
     * Méthode statique pour afficher rapidement un aperçu
     */
    public static void showPreview(String fileName, String bucketName, MinioClient minioClient) {
        MediaPreviewDialog dialog = new MediaPreviewDialog(fileName, bucketName, minioClient);
        dialog.show();
    }
}
