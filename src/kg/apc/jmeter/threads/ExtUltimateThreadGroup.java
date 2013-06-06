package kg.apc.jmeter.threads;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.testelement.TestListener;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author apc
 */
public class ExtUltimateThreadGroup
        extends AbstractSimpleThreadGroup
        implements Serializable, TestListener {
    //private static final Logger log = LoggingManager.getLoggerForClass();

    /**
     *
     */
    private static final Logger log = LoggingManager.getLoggerForClass();
    public static final String DATA_PROPERTY = "extendedultimatethreadgroupdata";
    private PropertyIterator scheduleIT;
    private CollectionProperty currentRecord;

    /**
     *
     */
    public ExtUltimateThreadGroup() {
        super();
    }

    @Override
    public void start(int groupCount, ListenerNotifier notifier, ListedHashTree threadGroupTree, StandardJMeterEngine engine) {
        running = true;

        int numThreads = getNumThreads();
        log.info("Starting thread group number " + groupCount
                + " threads " + numThreads);

        long now = System.currentTimeMillis(); // needs to be same time for all threads in the group
        final JMeterContext context = JMeterContextService.getContext();
        List<JMeterThread> threads = new ArrayList<JMeterThread>();
        for (int i = 0; running && i < numThreads; i++) {
            JMeterThread jmThread = makeThread(groupCount, notifier, threadGroupTree, engine, i, context);
            threads.add(jmThread);
        }

        if (running) {
            scheduleThread(now, threads);
        }

        for (int i = 0; running && i < numThreads; i++) {
            JMeterThread jmThread = threads.get(i);
            Thread newThread = new Thread(jmThread, jmThread.getThreadName());
            registerStartedThread(jmThread, newThread);
            newThread.start();
        }

        log.info("Started thread group number "+groupCount);
    }

    protected void scheduleThread(long tgStartTime, List<JMeterThread> threads) {
        Stack<JMeterThread> pendingThreads = new Stack<JMeterThread>();
        int timeCurr = 0;
        int threadIndex = 0;

        while (scheduleIT.hasNext()) {
            currentRecord = (CollectionProperty) scheduleIT.next();
            int timePrev = timeCurr;
            timeCurr = currentRecord.get(0).getIntValue();
            int numThreadsPrev = pendingThreads.size();
            int numThreadsCurr = currentRecord.get(1).getIntValue();

            for (int i = numThreadsPrev; i < numThreadsCurr; i++) {
                JMeterThread jmThread = threads.get(threadIndex++);
                long ascentPoint = tgStartTime + 1000 * timePrev;
                int rampUpDelayForThread = (int) Math.floor(1000 * (timeCurr - timePrev) * (double) (i - numThreadsPrev) / (numThreadsCurr - numThreadsPrev));
                long startTime = ascentPoint + rampUpDelayForThread;
                jmThread.setStartTime(startTime);
                pendingThreads.push(jmThread);
            }

            for (int i = numThreadsPrev - 1; i >= numThreadsCurr; i--) {
                JMeterThread jmThread = pendingThreads.pop();
                long descentPoint = tgStartTime + 1000 * timePrev;
                int rampDownDelayForThread = (int) Math.floor(1000 * (timeCurr - timePrev) * (double) (i - numThreadsPrev) / (numThreadsCurr - numThreadsPrev));
                long endTime = descentPoint + rampDownDelayForThread;
                jmThread.setEndTime(endTime);
                jmThread.setScheduled(true);
            }
        }

        while (pendingThreads.size() > 0) {
            JMeterThread jmThread = pendingThreads.pop();
            jmThread.setEndTime(Long.MAX_VALUE);
            jmThread.setScheduled(true);
        }
    }

    @Override
    protected void scheduleThread(JMeterThread thread, long tgStartTime) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return
     */
    public JMeterProperty getData() {
        //log.info("getData: "+getProperty(DATA_PROPERTY));
        JMeterProperty prop = getProperty(DATA_PROPERTY);
        return prop;
    }

    void setData(CollectionProperty rows) {
        //log.info("setData");
        setProperty(rows);
    }

    @Override
    public int getNumThreads() {
        int result = 0;

        JMeterProperty threadValues = getData();
        if (!(threadValues instanceof NullProperty)) {
            CollectionProperty columns = (CollectionProperty) threadValues;
            List<?> rows = (List<?>) columns.getObjectValue();
            Stack<Integer> threads = new Stack<Integer>();
            for (Iterator<?> iter = rows.iterator(); iter.hasNext();) {
                CollectionProperty prop = (CollectionProperty) iter.next();
                ArrayList<JMeterProperty> row = (ArrayList<JMeterProperty>) prop.getObjectValue();
                int numThreadsPrev = threads.size();
                int numThreadsCurr = row.get(1).getIntValue();
                for (int i = numThreadsPrev; i < numThreadsCurr; i++) {
                    threads.push(result++);
                }
                for (int i = numThreadsPrev - 1; i >= numThreadsCurr; i--) {
                    threads.pop();
                }
            }
        }

        return result;
    }

    @Override
    public void testStarted() {
        JMeterProperty data = getData();
        if (!(data instanceof NullProperty)) {
            scheduleIT = ((CollectionProperty) data).iterator();
        }
    }

    @Override
    public void testStarted(String host) {
        testStarted();
    }

    @Override
    public void testEnded() {
    }

    @Override
    public void testEnded(String host) {
        testEnded();
    }

    @Override
    public void testIterationStart(LoopIterationEvent event) {
    }
}
