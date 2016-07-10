import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.Label
import jdk.internal.org.objectweb.asm.MethodVisitor
import jdk.internal.org.objectweb.asm.Opcodes.*
import java.io.File
import java.util.*

class CommandCompiler {

    private var labels = Stack<Label>();

    private fun zeroCompile(mv: MethodVisitor){

        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitIntInsn(BIPUSH,0);
        mv.visitInsn(CASTORE);

    }

    private fun shiftCompile(mv: MethodVisitor, value: Int){ //Makes Array cyclical

        mv.visitVarInsn(ILOAD,2);
        mv.visitIntInsn(BIPUSH,value);
        mv.visitInsn(IADD);
        mv.visitInsn(DUP);
        val l = Label();
        val l2 = Label();

        mv.visitJumpInsn(IFGE,l);
        mv.visitIntInsn(SIPUSH,30000);
        mv.visitInsn(IADD);
        mv.visitJumpInsn(GOTO,l2);
        mv.visitLabel(l)
        mv.visitFrame(F_FULL, 4, arrayOf("[Ljava/lang/String;","[C", INTEGER,"java/util/Scanner"), 1, arrayOf(INTEGER));

        mv.visitIntInsn(SIPUSH,-30000);
        mv.visitInsn(IADD);
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFGE,l2);
        mv.visitIntInsn(SIPUSH,30000);
        mv.visitInsn(IADD);

        mv.visitLabel(l2)
        mv.visitFrame(F_FULL, 4, arrayOf("[Ljava/lang/String;","[C", INTEGER,"java/util/Scanner"), 1, arrayOf(INTEGER));
        mv.visitVarInsn(ISTORE,2);


    }

    private fun sumCompile(mv: MethodVisitor, value: Int){

        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitInsn(DUP2);
        mv.visitInsn(CALOAD);
        mv.visitIntInsn(BIPUSH,value);
        mv.visitInsn(IADD);
        mv.visitInsn(CASTORE);

    }

    private fun startLoopCompile(mv: MethodVisitor) {

        val ls = Label();
        val lf = Label();
        labels.push(lf);
        labels.push(ls);
        mv.visitLabel(ls);
        mv.visitFrame(F_FULL, 4, arrayOf("[Ljava/lang/String;","[C", INTEGER,"java/util/Scanner"), 0, null);
        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitInsn(CALOAD);
        mv.visitJumpInsn(IFEQ, lf);

    }

    private fun finishLoopCompile(mv: MethodVisitor){

        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitInsn(CALOAD);
        mv.visitJumpInsn(IFNE,labels.pop());
        mv.visitLabel(labels.pop());
        mv.visitFrame(F_FULL, 4, arrayOf("[Ljava/lang/String;","[C", INTEGER,"java/util/Scanner"), 0, null);

    }

    private fun printCompile(mv: MethodVisitor) {

        mv.visitFieldInsn(GETSTATIC, "java/lang/System","out","Ljava/io/PrintStream;");
        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitInsn(CALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(C)V", false);

    }

    private fun readCompile(mv: MethodVisitor) {

        mv.visitVarInsn(ALOAD,1);
        mv.visitVarInsn(ILOAD,2);
        mv.visitVarInsn(ALOAD,3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextByte", "()B", false);
        mv.visitInsn(CASTORE);

    }

    fun compile(className: String){

        val ch = CodeHandler(className + ".txt");
        ch.start();
        val commands = ch.commands;
        val cw = ClassWriter(0);
        cw.visit(V1_7,ACC_PUBLIC + ACC_SUPER,className,null,"java/lang/Object",null);
        var mv = cw.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD,0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,"main","([Ljava/lang/String;)V",null,null);
        mv.visitCode();
        mv.visitIntInsn(SIPUSH,30000);
        mv.visitIntInsn(NEWARRAY,T_CHAR);
        mv.visitVarInsn(ASTORE,1);

        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE,2);

        mv.visitTypeInsn(NEW,"java/util/Scanner");
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System","in","Ljava/io/InputStream;");
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitVarInsn(ASTORE,3);

        for(i in 0..(commands.size-1)) {
            when(commands[i].type){
                CommandType.SUM -> sumCompile(mv,commands[i].value);
                CommandType.SHIFT -> shiftCompile(mv,commands[i].value);
                CommandType.ZERO -> zeroCompile(mv);
                CommandType.START -> startLoopCompile(mv);
                CommandType.FINISH -> finishLoopCompile(mv);
                CommandType.PRINT -> printCompile(mv);
                CommandType.READ -> readCompile(mv);
            }
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 4);
        mv.visitEnd();
        cw.visitEnd();
        File(className + ".class").writeBytes(cw.toByteArray());
    }
}