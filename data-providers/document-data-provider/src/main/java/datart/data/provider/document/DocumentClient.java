package datart.data.provider.document;

import datart.core.data.provider.Dataframe;

public interface DocumentClient extends AutoCloseable {

    void ping();

    Dataframe execute(String command, long offset, int maxRows);

    @Override
    void close();
}
