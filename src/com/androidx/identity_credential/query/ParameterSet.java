package com.androidx.identity_credential.query;

import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.*;

import java.util.HashMap;
import java.util.Objects;

import static com.androidx.identity_credential.query.ICQueryExecutor.*;

public class ParameterSet extends HashMap<String, DataItem> {

    public static int DATE_TAG = 18013;  // Placeholder until real value is registered.

    public DataItem getParameter(DataItem queryEntry) throws QueryException {
        if (!(queryEntry instanceof Array)) {
            throw new QueryException(("Invalid parameter reference, must be an array"));
        }
        Array queryEntryArr = (Array) queryEntry;

        if (queryEntryArr.getDataItems().size() != 2) {
            throw new QueryException("Invalid parameter reference, must contain two data items");
        }
        UnicodeString name = (UnicodeString) queryEntryArr.getDataItems().get(0);
        Number type = (Number) queryEntryArr.getDataItems().get(1);

        DataItem parameter = super.get(name.getString());
        boolean typeMatched = false;
        switch (type.getValue().intValue()) {
            case TYPE_INTEGER:
                typeMatched = parameter instanceof Number;
                break;

            case TYPE_STRING:
                typeMatched = parameter instanceof UnicodeString;
                break;

            case TYPE_BOOLEAN:
                typeMatched =
                        parameter.equals(SimpleValue.TRUE) || parameter.equals((SimpleValue.FALSE));
                break;

            default:
                throw new QueryException(
                        "Invalid parameter reference, unknown type specifier " + type.getValue());
        }

        // Tags must match as well, if present.
        Tag paramRefTag = type.getTag();
        Tag paramValueTag = parameter.getTag();
        if (!Objects.equals(paramRefTag, paramValueTag)) {
            throw new QueryException("Invalid parameter reference, query type tag does not match " +
                                     "parameter type tag");
        }

        if (!typeMatched) {
            throw new QueryException(
                    "Invalid parameter reference, query type does not match " + "parameter type");
        }

        return parameter;
    }
}
