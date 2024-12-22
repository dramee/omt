import syspro.tm.Tasks;
import lexer.MyLexer;

public class Main {
    public static void main(String[] args){

        Tasks.Lexer.registerSolution(new MyLexer());
    }}
