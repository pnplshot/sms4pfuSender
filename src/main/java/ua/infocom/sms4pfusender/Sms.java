package ua.infocom.sms4pfusender;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
public class Sms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;
    private String message;
    private String status; //

    private String msgId;
    private Timestamp msgAdd;
    private Timestamp recived;

    private String errorId;
    private String errorMsg;
}