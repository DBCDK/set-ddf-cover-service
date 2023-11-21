package dk.dbc.setddfcover.model;

import java.util.Objects;

public class UpdateEvent {
    private String pid;
    private boolean coverExists;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
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
                ", pid='" + pid + '\'' +
                ", coverExists=" + coverExists +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateEvent that = (UpdateEvent) o;
        return coverExists == that.coverExists && Objects.equals(pid, that.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, coverExists);
    }
}
