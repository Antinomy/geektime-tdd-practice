package geektime.tdd.di.testData;

import geektime.tdd.di.ComponentProvider;
import geektime.tdd.di.ComponentRef;
import geektime.tdd.di.Context;

import java.util.ArrayList;
import java.util.List;

public  class PooledProvider<T> implements ComponentProvider<T> {
    private List<T> pools = new ArrayList<>();
    public static final int MAX =2;
    private int current;
    private ComponentProvider<T> provider;

    public PooledProvider(ComponentProvider<T> provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (pools.size() < MAX)
            pools.add( provider.get(context));

        return pools.get(current ++ % MAX);
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return provider.getDependencies();
    }
}