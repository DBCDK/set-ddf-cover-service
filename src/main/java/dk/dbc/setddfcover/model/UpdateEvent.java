package dk.dbc.setddfcover.model;

public class UpdateEvent {

    private String bibliographicRecordId;
    private boolean coverExists;

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public void setBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
    }

    public boolean isCoverExists() {
        return coverExists;
    }

    public void setCoverExists(boolean coverExists) {
        this.coverExists = coverExists;
    }
}
