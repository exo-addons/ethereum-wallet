package org.exoplatform.addon.ethereum.wallet.task.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.*;

import org.hibernate.annotations.DynamicUpdate;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "WalletTask")
@ExoEntity
@DynamicUpdate
@Table(name = "ADDONS_WALLET_TASK")
@NamedQueries({
    @NamedQuery(name = "WalletTask.getUserTasks", query = "SELECT wt FROM WalletTask wt WHERE wt.completed = FALSE AND (wt.assignee IS EMPTY OR wt.assignee = :assignee)"),
})
public class WalletTaskEntity implements Serializable {

  private static final long serialVersionUID = 4475704534821391132L;

  @Id
  @SequenceGenerator(name = "SEQ_WALLET_TASK_ID", sequenceName = "SEQ_WALLET_TASK_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_WALLET_TASK_ID")
  @Column(name = "TASK_ID")
  private Long              id;

  @Column(name = "TASK_MESSAGE")
  private String            message;

  @Column(name = "TASK_TYPE", nullable = false)
  private String            type;

  @Column(name = "TASK_LINK", nullable = false)
  private String            link;

  @Column(name = "TASK_ASSIGNEE")
  private long              assignee;

  @Column(name = "TASK_COMPLETED")
  private Boolean           completed;

  @ElementCollection
  @CollectionTable(name = "ADDONS_WALLET_TASK_PARAMETERS", joinColumns = @JoinColumn(name = "TASK_ID"))
  @Column(name = "TASK_PARAMETER_ID")
  private List<String>      parameters;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public long getAssignee() {
    return assignee;
  }

  public void setAssignee(long assignee) {
    this.assignee = assignee;
  }

  public Boolean getCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public List<String> getParameters() {
    return parameters;
  }

  public void setParameters(List<String> parameters) {
    this.parameters = parameters;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

}
