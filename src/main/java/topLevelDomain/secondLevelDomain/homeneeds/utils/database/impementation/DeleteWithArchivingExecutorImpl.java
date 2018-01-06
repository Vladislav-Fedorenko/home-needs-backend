package topLevelDomain.secondLevelDomain.homeneeds.utils.database.impementation;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.sql.Timestamp;
import java.time.Instant;

import topLevelDomain.secondLevelDomain.homeneeds.utils.archive.ArchivedException;
import topLevelDomain.secondLevelDomain.homeneeds.utils.database.exeception.DatabaseTasksExecutorException;
import topLevelDomain.secondLevelDomain.homeneeds.utils.database.extending.DeleteExecutor;
import topLevelDomain.secondLevelDomain.homeneeds.utils.entities.IArchiveEntity;
import topLevelDomain.secondLevelDomain.homeneeds.utils.entities.IArchiveEntityException;
import topLevelDomain.secondLevelDomain.homeneeds.utils.entities.IEntity;
import topLevelDomain.secondLevelDomain.homeneeds.utils.timestamp.TimestampException;

public class DeleteWithArchivingExecutorImpl<T extends IEntity, U extends IArchiveEntity>
  implements DeleteExecutor<T, U> {

  private SessionFactory sessionFactory;
  private Session session;

  private U archivedObject;
  private Class<T> classOfDeletedObject;
  private Long idOfDeletedObject;
  private boolean resultOfDeleting;

  public DeleteWithArchivingExecutorImpl(final SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void execute() throws DatabaseTasksExecutorException {
    try {
      session = sessionFactory.openSession();
      session.beginTransaction();

      Timestamp timestamp = getTimestamp();
      T deletedObject = getDeletedObjectFromDatabase();
      prepareObjectToArchive(deletedObject, timestamp);
      session.save(archivedObject);
      session.delete(deletedObject);
      session.getTransaction().commit();
      session.evict(deletedObject);

      resultOfDeleting = isDeleted();
    } catch (IArchiveEntityException iArchiveEntityException) {
      throw new DatabaseTasksExecutorException(
        "Failed deleting. Errors of setting values of fields from entity to archive_entity",
        iArchiveEntityException
      );
    } catch (TimestampException timestampException) {
      throw new DatabaseTasksExecutorException(
        "Failed deleting. Errors of setting timestamp of creating to archive_entity",
        timestampException
      );
    } catch (ArchivedException archivedException) {
      throw new DatabaseTasksExecutorException(
        "Failed deleting. Errors of setting reason of archiving to archive_entity",
        archivedException
      );
    } finally {
      session.close();
    }
  }

  private Timestamp getTimestamp() {
    return Timestamp.from(Instant.now());
  }

  private T getDeletedObjectFromDatabase() {
    return session.get(classOfDeletedObject, idOfDeletedObject);
  }

  private void prepareObjectToArchive(final T deletedObject, final Timestamp timestamp)
    throws IArchiveEntityException, TimestampException, ArchivedException, DatabaseTasksExecutorException {

    if(deletedObject == null) {
      throw new DatabaseTasksExecutorException(
        "Failed deleting. Object with id = " + idOfDeletedObject + " not found in database"
      );
    }
    archivedObject.setValuesOfFieldsFromEntity(deletedObject);
    archivedObject.setCreatedAt(timestamp);
    archivedObject.setArchivingReason("delete");
  }

  private boolean isDeleted() {
    return getDeletedObjectFromDatabase() == null;
  }

  @Override
  public void setId(final Long id) throws DatabaseTasksExecutorException {
    try {
      this.idOfDeletedObject = id;
    } catch (Exception e) {
      throw new DatabaseTasksExecutorException("Failed of setting deleted object's id", e);
    }
  }

  @Override
  public void setArchivedObject(final U archivedObject) throws DatabaseTasksExecutorException {
    try {
      this.archivedObject = archivedObject;
    } catch (Exception e) {
      throw new DatabaseTasksExecutorException("Failed of setting archived object", e);
    }
  }

  @Override
  public void setClassOfDeletedObject(final Class<T> classOfDeletedObject) throws DatabaseTasksExecutorException {
    try {
      this.classOfDeletedObject = classOfDeletedObject;
    } catch (Exception e) {
      throw new DatabaseTasksExecutorException("Failed of setting deleted object's class", e);
    }
  }

  @Override
  public boolean getResult() throws DatabaseTasksExecutorException {
    try {
      return resultOfDeleting;
    } catch (Exception e) {
      throw new DatabaseTasksExecutorException("Failed of getting deleting's result", e);
    }
  }
}
