package dk.dbc.setddfcover;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Date;

@Entity
@NamedQueries({
        @NamedQuery(
                name = CoverEntity.SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_NAME,
                query = CoverEntity.SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_QUERY,
                lockMode = LockModeType.PESSIMISTIC_WRITE
        )
})
@Table(name = "cover")
public class CoverEntity {
    public static final String SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_NAME = "select.from.cover";
    public static final String SELECT_FROM_COVER_BY_BIBLIOGRAPHICRECORDID_QUERY =
            "SELECT c FROM CoverEntity c " +
                    "WHERE c.bibliographicRecordId = :bibliographicRecordId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String bibliographicRecordId;
    private boolean coverExists;
    private String agencyId;
    private Date modified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public Date getModified() {
        return new Date(modified.getTime());
    }

    public void setModified(Date modified) {
        this.modified = new Date(modified.getTime());
    }

}
