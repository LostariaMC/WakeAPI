package fr.lostaria.wakeapi.core.exception;

public class OvhApiException extends Exception {

    private final OvhApiExceptionCause ovhCause;

    public OvhApiException(String message, OvhApiExceptionCause ovhCause) {
        super(message);
        this.ovhCause = ovhCause;
    }

    public OvhApiExceptionCause getOvhCause() {
        return ovhCause;
    }

    @Override
    public String toString() {
        return "OvhApiException [ovhCause=" + ovhCause + "] : " + getLocalizedMessage();
    }

}
