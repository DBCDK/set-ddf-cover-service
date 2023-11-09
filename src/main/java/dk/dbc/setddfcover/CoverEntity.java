package dk.dbc.setddfcover;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@NamedQueries({
        @NamedQuery(
                name = CoverEntity.SELECT_FROM_COVER_BY_PID_NAME,
                query = CoverEntity.SELECT_FROM_COVER_BY_PID_QUERY,
                lockMode = LockModeType.PESSIMISTIC_WRITE
        )
})
@Table(name = "cover")
public class CoverEntity {
    public static final String SELECT_FROM_COVER_BY_PID_NAME = "select.from.cover";
    public static final String SELECT_FROM_COVER_BY_PID_QUERY =
            "SELECT c FROM CoverEntity c " +
                    "WHERE c.pid = :pid";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String pid;
    private boolean coverExists;
    private Date modified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Date getModified() {
        return new Date(modified.getTime());
    }

    public void setModified(Date modified) {
        this.modified = new Date(modified.getTime());
    }

}
