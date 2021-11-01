package dk.dbc.setddfcover.model;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "UpdateEvent{" +
                "bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", coverExists=" + coverExists +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateEvent that = (UpdateEvent) o;
        return coverExists == that.coverExists && Objects.equals(bibliographicRecordId, that.bibliographicRecordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bibliographicRecordId, coverExists);
    }
}
