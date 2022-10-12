package geektime.tdd.di;

import java.util.List;

class SingletonProvider<T> implements ComponentProvider<T> {
    private T singleton;
    private ComponentProvider provider;

    public SingletonProvider(ComponentProvider provider) {
        this.provider = provider;
    }

    @Override
    public T get(Context context) {
        if (singleton == null)
            singleton = (T) provider.get(context);

        return singleton;
    }

    @Override
    public List<ComponentRef> getDependencies() {
        return provider.getDependencies();
    }
}
