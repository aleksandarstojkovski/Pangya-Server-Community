using PangyaAPI.Utilities;
using PangyaAPI.Utilities.Log;
using System;
using System.Threading;
using static PangyaAPI.Utilities.Tools;

public class PangyaThread : IDisposable
{
    private Thread m_thread;
    private ThreadRoutine m_routine;
    private object m_parameter;

    private ManualResetEventSlim m_pauseEvent;
    private volatile bool m_running;

    private uint m_tipo;

    public PangyaThread(uint tipo)
    {
        m_tipo = tipo;
        m_pauseEvent = new ManualResetEventSlim(true);
    }

    public PangyaThread(
        uint tipo,
        ThreadRoutine routine,
        object parameter,
        ThreadPriority priority = ThreadPriority.Normal
    ) : this(tipo)
    {
        init_thread(routine, parameter, priority);
    }

    public void init_thread(
        ThreadRoutine routine,
        object parameter,
        ThreadPriority priority = ThreadPriority.Normal
    )
    {
        if (isLive())
            throw new exception("Thread ja esta iniciada.");

        m_routine = routine ?? throw new exception("Routine is null");
        m_parameter = parameter;

        m_running = true;

        m_thread = new Thread(thread_entry);
        m_thread.IsBackground = true;
        m_thread.Priority = priority;
        m_thread.Start();
    }

    private void thread_entry()
    {
        try
        {
            // comportamento idêntico ao C++
            m_pauseEvent.Wait();

            m_routine?.Invoke(m_parameter);
        }
        catch (ThreadAbortException)
        {
            // ignore
        }
        catch (Exception e)
        {
            _smp.message_pool.getInstance().push(
                new message("[Thread][ErrorSystem] " + e, type_msg.CL_FILE_LOG_AND_CONSOLE)
            );
        }
        finally
        {
            m_running = false;
        }
    }

    public void pause_thread()
    {
        if (!isLive())
            throw new exception("Thread nao inicializada");

        m_pauseEvent.Reset();
    }

    public void resume_thread()
    {
        if (!isLive())
            throw new exception("Thread nao inicializada");

        m_pauseEvent.Set();
    }

    public void exit_thread()
    {
        if (!isLive())
            return;

        m_running = false;

        try
        {
            m_thread.Interrupt();
        }
        catch { }

        try
        {
            m_thread.Join(1000);
        }
        catch { }

        m_thread = null;
    }

    public void waitThreadFinish(int milliseconds)
    {
        if (m_thread != null && !m_thread.Join(milliseconds))
            throw new exception("Erro ao esperar a thread finalizar");
    }

    public bool isLive()
    {
        return m_thread != null && m_thread.IsAlive;
    }

    public uint getTipo()
    {
        return m_tipo;
    }

    public void Dispose()
    {
        exit_thread();
    }
}
