package org.exoplatform.addon.ethereum.wallet.migration;

import java.util.ArrayList;
import java.util.List;

import org.web3j.abi.*;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;

public class ContractUtils {

  private ContractUtils() {
  }

  @SuppressWarnings("rawtypes")
  public static EventValues staticExtractEventParameters(Event event, Log log) {

    List<String> topics = log.getTopics();
    String encodedEventSignature = EventEncoder.encode(event);
    if (!topics.get(0).equals(encodedEventSignature)) {
      return null;
    }

    List<Type> indexedValues = new ArrayList<>();
    List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());

    List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
    for (int i = 0; i < indexedParameters.size(); i++) {
      Type value = FunctionReturnDecoder.decodeIndexedValue(topics.get(i + 1), indexedParameters.get(i));
      indexedValues.add(value);
    }
    return new EventValues(indexedValues, nonIndexedValues);
  }

}
