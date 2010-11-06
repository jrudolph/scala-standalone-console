package virtualvoid.scala;

import java.io.*;
import java.util.jar.*;
import java.util.zip.*;
import java.net.*;
import java.lang.reflect.*;
import com.sun.jna.*;

public class ScalaConsoleLauncher {
    PrintStream out;

    public void log(String format, Object...args) {
        System.out.println(String.format(format, args));
    }

    private static File extract(InputStream is) throws IOException {
        File tempFile = File.createTempFile("scala-console",".jar");
        tempFile.deleteOnExit();
        OutputStream os = new FileOutputStream(tempFile);
        JarOutputStream jos = new JarOutputStream(os);

        Pack200.newUnpacker().unpack(is, jos);
        jos.close();
        os.close();
        
        return tempFile;
    }

    public static OutputStream getUnderlying(FilterOutputStream ps) throws Exception {
        Field outF = FilterOutputStream.class.getDeclaredField("out");
        outF.setAccessible(true);
        OutputStream res = (OutputStream) outF.get(ps);
        if (res instanceof FilterOutputStream)
            return getUnderlying((FilterOutputStream) res);
        else
            return res;
    }

    public static void set(Object o, String name, Object value) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }

    static {
        Native.register("kernel32");
    }
    public static native int AllocConsole();
    public static native int GetStdHandle(int type);

    public static FileDescriptor descriptor(int desc, int handle) throws Exception {
        FileDescriptor res = new FileDescriptor();
        set(res, "fd", desc);
        set(res, "handle", handle);
        return res;
    }
    public void createConsoleAndRedirectStds() throws Exception {
        AllocConsole();
        log("In-Handle: %d", GetStdHandle(-10));
        log("Out-Handle: %d", GetStdHandle(-11));
        
        FileInputStream is = new FileInputStream(descriptor(-1,GetStdHandle(-10)));
        PrintStream os = new PrintStream(new FileOutputStream(descriptor(-1, GetStdHandle(-11))));
        System.setIn(is);
        System.setOut(os);
    }

    public static void main(String[] args) throws Exception {
        ScalaConsoleLauncher main = new ScalaConsoleLauncher();
        OutputStream os = new FileOutputStream("out.log");
        main.out = new PrintStream(os);

        try {
            main.createConsoleAndRedirectStds();
            main.run();
        } catch(Throwable t) {
            t.printStackTrace(main.out);
        } finally {
            os.close();
        }
    }

    public void run() throws Exception {      
        InputStream packFile = ScalaConsoleLauncher.class.getClassLoader().getResourceAsStream("scala.pack.gz");
        log("Extracting Scala...");

        File unpacked = extract(new GZIPInputStream(packFile));
        log("Extracted to %s (%d bytes)", unpacked.getAbsolutePath(), unpacked.length());

        ClassLoader myClassLoader = new URLClassLoader(new URL[] {unpacked.toURI().toURL() }) {
            @Override
            protected synchronized Class<?> loadClass(String name,
                                                      boolean resolve) throws ClassNotFoundException {
                Class<?> cl = findLoadedClass(name);
                if (cl == null)
                    try {
                        return findClass(name);
                    } catch (ClassNotFoundException e) {
                        return super.loadClass(name, resolve);
                    }
                else
                    return cl;
            }
        };
        Class<?> runner = myClassLoader.loadClass("scala.tools.nsc.MainGenericRunner");
        Method main = runner.getMethod("main", String[].class);
        main.invoke(null, (Object) new String[] {"-bootclasspath", unpacked.getAbsolutePath() });
    }
}