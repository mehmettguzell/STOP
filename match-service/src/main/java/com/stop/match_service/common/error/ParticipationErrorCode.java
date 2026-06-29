package com.stop.match_service.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ParticipationErrorCode implements ErrorCode {
    MATCH_NOT_OPEN("MATCH_NOT_OPEN", "Mac yeni katilim isteklerine kapali.", HttpStatus.BAD_REQUEST),
    MATCH_FULL("MATCH_FULL", "Mac maksimum kapasitesine ulasmistir.", HttpStatus.BAD_REQUEST),
    ALREADY_JOINED("ALREADY_JOINED", "Bu maca zaten katildiniz.", HttpStatus.BAD_REQUEST),
    ALREADY_REQUESTED("ALREADY_REQUESTED", "Bu mac icin zaten bekleyen bir isteginiz var.", HttpStatus.BAD_REQUEST),
    REQUEST_NOT_FOUND("REQUEST_NOT_FOUND", "Katilim istegi bulunamadi.", HttpStatus.NOT_FOUND),
    INVALID_REQUEST_STATUS("INVALID_REQUEST_STATUS", "Bu islem icin istek durumu uygun degil.", HttpStatus.BAD_REQUEST),
    NOT_JOINED("NOT_JOINED", "Bu maca katilimci olarak kayitli degilsiniz.", HttpStatus.BAD_REQUEST),
    ORGANIZER_CANNOT_LEAVE("ORGANIZER_CANNOT_LEAVE", "Organizator olarak mactan ayrilamazsiniz. Once organizatorlugu devredin veya maci iptal edin.", HttpStatus.BAD_REQUEST),
    TOO_LATE_TO_LEAVE("TOO_LATE_TO_LEAVE", "Mactan ayrilmak icin cok gec. Mac baslangicina 2 saatten az kaldiginda ayrilamazsiniz.", HttpStatus.BAD_REQUEST),
    INVALID_MATCH_STATUS_FOR_LEAVING("INVALID_MATCH_STATUS_FOR_LEAVING", "Baslamis, tamamlanmis veya iptal edilmis bir mactan ayrilamazsiniz.", HttpStatus.BAD_REQUEST),
    MAX_REJECTIONS_REACHED("MAX_REJECTIONS_REACHED", "Bu mac icin maksimum red sayisina ulasiniz. Tekrar istek gonderemezsiniz.", HttpStatus.BAD_REQUEST),
    INVALID_TEAM_ASSIGNMENT("INVALID_TEAM_ASSIGNMENT", "Takim atamasi gecersiz. Tum katilimcilar tam olarak bir kez atanmalidir.", HttpStatus.BAD_REQUEST),
    TEAM_ASSIGNMENT_NOT_ALLOWED("TEAM_ASSIGNMENT_NOT_ALLOWED", "Bu mac durumunda takim atamasi yapilamaz.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
