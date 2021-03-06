package org.exoplatform.addon.ethereum.wallet.model;

import java.io.Serializable;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MinedTransactionDetail implements Serializable {

  private static final long serialVersionUID = -1895498188938300594L;

  private String            hash;

  private String            blockHash;

  private long              blockTimestamp;
}
