package com.androidx.identity_credential.query;

import co.nstant.in.cbor.model.DataItem;

public interface DataSet {
    DataItem getDataElement(DataItem entry);
}
