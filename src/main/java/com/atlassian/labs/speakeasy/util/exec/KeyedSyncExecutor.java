package com.atlassian.labs.speakeasy.util.exec;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.atlassian.labs.speakeasy.util.ExtensionValidate.isValidExtensionKey;

/**
 *
 */
public abstract class KeyedSyncExecutor<T, D>
{

    private final Map<String,ReadWriteLock> locks = new MapMaker().makeComputingMap(new Function<String,ReadWriteLock>()
    {
        public ReadWriteLock apply(String from)
        {
            return new ReentrantReadWriteLock();
        }
    });

    public final <R> R forKey(String id, D targetContext, Operation<T, R> op)
    {
        if (id == null)
        {
            try
            {
                R result = op.operateOn(null);
                afterSuccessfulOperation(null, result);
                return result;
            }
            catch (Exception e)
            {
                handleException(null, e);
            }
        }

        // only sync with git repo if a valid plugin key
        if (allowKey(id))
        {
            ReadWriteLock readWriteLock = locks.get(id);
            Lock lock = op instanceof ReadOnlyOperation ? readWriteLock.readLock() : readWriteLock.writeLock();
            lock.lock();
            try
            {
                T target = getTarget(id, targetContext);
                R result = op.operateOn(target);
                afterSuccessfulOperation(target, result);
                return result;
            }
            catch (Exception e)
            {
                handleException(id, e);
            }
            finally
            {
                lock.unlock();
            }
        }

        return null;
    }

    protected void afterSuccessfulOperation(T target, Object result)
    {}

    protected boolean allowKey(String id)
    {
        return true;
    }

    protected void handleException(String id, Exception ex)
    {
        throw new RuntimeException(ex);
    }

    protected abstract T getTarget(String id, D targetContext) throws Exception;




}
