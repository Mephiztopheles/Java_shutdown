package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.joda.time.DateTime;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimerTask;

import static javafx.scene.control.ButtonBar.ButtonData;

public class Controller implements Initializable {
    @FXML
    private Button shutdown;
    @FXML
    private ChoiceBox minutes;
    @FXML
    private ChoiceBox hours;
    @FXML
    private Label lblHours;

    private ObservableList<String> minuteList = FXCollections.observableArrayList();
    private ObservableList<String> hourList = FXCollections.observableArrayList();
    private String operatingSystem = System.getProperty( "os.name" );
    private java.util.Timer t;
    private boolean timerRunning = false;

    @SuppressWarnings( "unchecked" )
    @Override
    public void initialize( URL location, ResourceBundle resources ) {

        Integer min = 0;
        while ( min < 60 ) {
            minuteList.add( min <= 9 ? "0" + min : min.toString() );
            min++;
        }
        Integer h = 0;
        while ( h < 24 ) {
            hourList.add( h <= 9 ? "0" + h : h.toString() );
            h++;
        }

        hours.setItems( hourList );
        hours.setValue( hourList.get( 0 ) );
        minutes.setItems( minuteList );
        minutes.setValue( minuteList.get( 5 ) );
    }

    public void checkDisabled() {

        Object m = minutes.getValue();
        Object h = hours.getValue();
        if ( m == null || m.equals( "" ) || h == null || h.equals( "" ) ) {
            shutdown.setDisable( true );
        } else {
            shutdown.setDisable( false );
        }
    }

    public void shutDown() {

        Integer h = Integer.parseInt( (String) hours.getValue() );
        Integer m = Integer.parseInt( (String) minutes.getValue() );
        final Integer seconds = ( m + ( h * 60 ) ) * 60;

        if ( seconds == 0 ) {
            Alert alert = new Alert( AlertType.CONFIRMATION );
            ButtonType yes = new ButtonType( "Ja", ButtonData.OK_DONE );
            ButtonType no = new ButtonType( "Nein", ButtonData.CANCEL_CLOSE );
            alert.setTitle( "Achtung" );
            alert.setHeaderText( null );
            alert.setContentText( "Wollen Sie den Computer jetzt herunterfahren?" );
            alert.getButtonTypes().setAll( yes, no );

            Optional<ButtonType> result = alert.showAndWait();
            if ( !( result.isPresent() && result.get() == yes ) ) {
                return;
            }
        }

        String shutdownCommand;

        if ( isUnix() ) {
            shutdownCommand = "shutdown -h " + seconds;
        } else if ( isWindows() ) {
            shutdownCommand = "shutdown.exe -s -t " + seconds;
        } else {
            throw new RuntimeException( "Unsupported operating system: " + operatingSystem );
        }

        try {
            Runtime.getRuntime().exec( shutdownCommand );
            t = new java.util.Timer();
            timerRunning = true;
            t.schedule( new TimerTask() {
                private Integer s = seconds;

                @Override
                public void run() {
                    if ( s < 0 ) {
                        timerRunning = false;
                        t.cancel();
                        return;
                    }
                    DateTime end = new DateTime( 2000, 1, 1, 0, 0, 0, 0 );
                    end = end.plusSeconds( s );
                    final Date date = new Date( end.getMillis() );
                    final SimpleDateFormat sdf = new SimpleDateFormat( "HH:mm:ss" );
                    Platform.runLater( () -> lblHours.setText( sdf.format( date ) ) );
                    s--;
                }
            }, 0, 1000 );

        } catch ( IOException e ) {
            e.printStackTrace();
        }

        DateTime end = new DateTime( 2000, 1, 1, 0, 0, 0, 0 );
        end = end.plusSeconds( seconds );
        Date date = new Date( end.getMillis() );
        SimpleDateFormat sdf = new SimpleDateFormat( "HH:mm:ss" );

        displayTray( "Herunterfahren", "Der PC wird in " + sdf.format( date ) + " automatisch heruntergefahren" );

    }

    public void cancel() {

        String shutdownCommand;
        if ( isUnix() ) {
            shutdownCommand = "shutdown -a ";
        } else if ( isWindows() ) {
            shutdownCommand = "shutdown.exe -a ";
        } else {
            throw new RuntimeException( "Unsupported operating system." );
        }
        if ( timerRunning )
            displayTray( "Herunterfahren abgebrochen", "Der PC wird nicht mehr automatisch heruntergefahren" );
        try {
            Runtime.getRuntime().exec( shutdownCommand );
            lblHours.setText( "" );
            if ( timerRunning ) {
                t.cancel();
                timerRunning = false;
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private boolean isWindows() {
        return operatingSystem.matches( "Windows.*" );
    }

    private boolean isUnix() {
        return operatingSystem.matches( "Linux.*" ) || operatingSystem.matches( "Mac.*" );
    }

    private void displayTray( String caption, String text ) {

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage( "icon.png" );
            TrayIcon trayIcon = new TrayIcon( image, "" );

            trayIcon.setImageAutoSize( true );
            trayIcon.displayMessage( caption, text, TrayIcon.MessageType.INFO );

            tray.add( trayIcon );
        } catch ( AWTException e ) {
            e.printStackTrace();
        }

    }
}
