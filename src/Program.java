
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
/**
 * Created by anvob on 13.10.12.
 */
public class Program extends Frame implements ActionListener
{
    private BufferedWriter file; 	// ссылка на объект файла
    private ArrayList<Name> listName;	//Список имен(таблица имен)
    private ArrayList<Tree> treeList;
    private Stack<Operand> Operands; //Стек операндов
    private Stack<Character> Functions;	//Стек ограничителей
    private int [] pos = new int[1];	// ИНдикатор текущей позиции
    private int j=1;	//ИНдикатор номера операции
    private int base;
    private int numLink=1;
    private String outStr="Результат:\n";	//Итоговая строка
    private TextArea tField;	//Редактор выражения
    private TextArea tArea;// Редактор для вывода результата
    private Button bPerform;	//Кнопка "ОК"
    private Checkbox chBox;	//переключатель "В файл"
    /* Конструктор */
    public Program()
    {
        setLayout(null);
        listName = new ArrayList<Name>();
        treeList = new ArrayList<Tree>();
        Name n = new Name("false", false);
        listName.add(n);
        n = new Name("true", true);
        listName.add(n);
        Functions = new Stack<Character>();
        Operands = new Stack<Operand>();
        //Создание Переключателя
        chBox= new Checkbox("В файл");
        chBox.setLocation(565, 50);
        chBox.setSize(70, 20);
        add(chBox);
        //Создание поля редактора вырожения
        tField = new  TextArea( "Введите выражение",1,1,1);
        tField.setLocation(10,30);
        tField.setSize(550, 40);
        add(tField);
        //СОздание поля для вывода результата
        tArea = new TextArea("Входной язык содержит логические выражения, разделенные символом; (точка с запятой)." +
                " Логические выражения состоят из идентификаторов(все буквы или цифры во всех возможный вариантах), " +
                "констант true и false, знака присваивания (:= или =), " +
                "знаков операций or (|,||), xor (^), and (&,&&), not (!) и круглых скобок.",1,1,1);
        tArea.setLocation(10,70);
        tArea.setSize(620, 400);
        add(tArea);
        //создание кнопки
        bPerform = new Button("ОК");
        bPerform.setSize(70, 20);
        bPerform.setLocation(560, 30);
        bPerform.setActionCommand("OK");
        bPerform.addActionListener(this);
        this.add(bPerform);

        //Обработчие закрытия окна
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent wE)
            {
                System.exit(0);
            }
        });
    }
    /* Реакция на нажатие клавиши */
    @Override
    public void actionPerformed(ActionEvent aE)
    {
        String str = aE.getActionCommand();
        if (str.equals("OK"))
        {
            treeList.clear();
            base=0;
            numLink=1;
            if(chBox.getState())
                try	// создание объекта файла
                {
                    file = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream("reportFile.txt", false)));
                }
                catch (Exception e1){ e1.printStackTrace(); }
            Functions.clear();
            Operands.clear();
            try { Calculation(tField.getText()); }
            catch (Exception e1) { e1.printStackTrace(); }
            outStr="Результат:\n";
            j=1;
            repaint();

        }

    }
    /* Ф-я вычисления */
    public void Calculation(String str) throws Exception
    {
        str= "( "+str;//+" )";
        pos[0]=0;
        Object token;
        Object prevToken='(';
        do
        {
            token=getToken(str, pos);
            if(token!=null&&prevToken.equals("end"))Functions.push('(');
            if(token==null&&!prevToken.equals("end"))
            {
                PrintFile("Ошибка! Не правильная запись выражения, пропущена ;");
                throw new Exception("Ошибка! Не правильная запись выражения, пропущена ;");
            }
            //Работа над ошибками
            if(token instanceof Character&&prevToken instanceof Character)
            {
                //Проверка на повтор операторов которые не должны идти подряд
                if(((char)prevToken=='='||(char)prevToken=='&'||(char)prevToken=='|'||(char)prevToken=='('||(char)prevToken=='^')&&((char)token=='&'||(char)token=='|'||(char)token=='='||(char)token==')'||(char)token=='^'))
                {
                    PrintFile("Ошибка! Неправильная запись выражения: Неверная запись ограничителей! \""+(char)prevToken+(char)token+"\"\n");
                    throw new Exception("\nНеправильная запись выражения: Неверная запись ограничителей! \""+(char)prevToken+(char)token+"\"\n");
                }
            }
            // разруливаем унарное отрицание !
            if (token instanceof Character && prevToken instanceof Character &&(char)token == '!')
                Operands.push(new Operand("", false,-1)); // Добавляем нулевой элемент
            //Если текущий объект Операнд
            if(token instanceof Operand)
            {
                // Если операнд стоит после ")"
                if(prevToken instanceof Character&&(char)prevToken==')')
                {
                    PrintFile("Ошибка! Неправильная запись выражения: Неверная запись ограничителей! \""+(char)prevToken+((Operand)token).id+"\"");
                    throw new Exception("Неправильная запись выражения: Неверная запись ограничителей! \""+(char)prevToken+((Operand)token).id+"\"");
                }
                // Если операнд стоит после операнда
                if(token instanceof Operand&&prevToken instanceof Operand)
                {
                    PrintFile("Ошибка! Неправильная запись выражения: Неверная запись операндов! "+((Operand)prevToken).id+" "+((Operand)token).id);
                    throw new Exception("Неправильная запись выражения: Неверная запись операндов! "+((Operand)prevToken).id+" "+((Operand)token).id);
                }
                Operands.push((Operand) token);
            }
            else
            if(token instanceof Character)
            {
                System.out.print(token);
                if ((char)token == ')')//Если новый ограничитель ")"
                {
                    // Скобка - исключение из правил. выталкивает все операции до первой открывающейся
                    while (Functions.size() > 0 && Functions.peek() != '(')
                        popFunction(Operands, Functions);
                    try
                    {
                        Functions.pop(); // Удаляем саму скобку "("
                    }
                    // Если стек функций уже пуст (В выражении пропущена "(" )
                    catch(Exception e)
                    {
                        PrintFile("Ошибка! Неправильная запись выражения: Неверно расставлены скобки!");
                        throw new Exception("Неправильная запись выражения: Неверно расставлены скобки!");
                    }
                }
                else
                {
                    if(prevToken instanceof Operand&&(char)token=='(')// Если перед "(" стоит операнд
                    {
                        PrintFile("Ошибка! Неправильная запись выражения: Неверная запись ограничителей! \""+((Operand)prevToken).id+(char)token+"\"\n");
                        throw new Exception("Неправильная запись выражения: Неверная запись ограничителей! \""+((Operand)prevToken).id+(char)token+"\"\n");
                    }
                    while (canPop((char)token, Functions)) // Если можно вытолкнуть
                        popFunction(Operands, Functions); // то выталкиваем
                    Functions.push((char)token); // Кидаем новую операцию в стек
                }
            }
            else// для конца выражения ;
            {
                if(token instanceof String)//если точка с запятой
                {
                    {
                        while (Functions.size() > 0 && Functions.peek() != '(')
                            popFunction(Operands, Functions);
                        try
                        {
                            Functions.pop(); // Удаляем саму скобку "("
                        }
                        // Если стек функций уже пуст (В выражении пропущена "(" )
                        catch(Exception e)
                        {
                            PrintFile("Ошибка! Неправильная запись выражения: Неверно расставлены скобки!");
                            throw new Exception("Неправильная запись выражения: Неверно расставлены скобки!");
                        }
                        pos[0]++;
                        //if())Functions.push('(');
                    }
                }
            }
            prevToken = token;
        }
        while(token!=null);
        Operand op=Operands.pop();
        //if(!Operands.isEmpty())throw new Exception("Неправильная запись выражения: Неверно расставлены скобки!"+Operands.peek().id);
        if(!Functions.isEmpty())
        {
            PrintFile("Ошибка! Неправильная запись выражения: Неверно расставлены скобки!");
            throw new Exception("Неправильная запись выражения: Неверно расставлены скобки!");
        }
        PrintFile("\nОтвет:"+ op.id +" = "+op.value);
        PrintFile("\nТаблица имен:");
        base=op.ownlink;
        for(int i=0;i<listName.size();i++)
        {
            Name n0 = (Name)listName.get(i);
            PrintFile(n0.id+":="+n0.value);
        }
        for(int i=0;i<treeList.size();i++)
        {
            Tree t0 = (Tree)treeList.get(i);
            System.out.println("\n"+t0.name+":="+t0.ownLink+" "+t0.l_link+" "+t0.r_link);
        }
        repaint();
    	/*Закрытие файла если chBox.state() = true*/
        if(chBox.getState())
            try
            {
                file.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
    }
    /* Ф-я определения очередности выполнения операций */
    public Boolean canPop(char op1, Stack<Character> Functions) throws Exception
    {
        int p1,p2;
        if (Functions.size() == 0)return false;
        p1 = getPriority(op1);
        p2 = getPriority(Functions.peek());
        if(p1!=0||p2!=0)
        {
            return p1 >= 0 && p2 >= 0 && p1 >= p2;
        }
        else return false;
    }
    /* Функция проверки приоритетов ограничителей */
    public int getPriority(char op) throws Exception
    {
        int k=-2;
        if(op=='(')k =-1; // не выталкивает сам и не дает вытолкнуть себя другим
        if(op=='!')k = 0;
        if(op=='&')k = 1;
        if(op=='|')k = 2;
        if(op=='^')k = 3;
        if(op=='=')k = 4;
		/* Ошибка, если введена неизвестная операция (\/%$#@-_+?.*:"'~ и 	т.д.) */
        if(k==-2)
        {
            PrintFile("Ошибка! Недопустимая операция: \""+op+"\"");
            throw new Exception("Ошибка! Недопустимая операция: \""+op+"\"");
        }
        return k;
    }
    /* Ф-я выполнения операций */
    public void popFunction(Stack<Operand> Operands, Stack<Character> Functions) throws Exception
    {
        boolean A,B;
        int n_a=0,n_b=0;
        char c;
        Tree t;
		/* Получение необходимых операндов */
        c = Functions.pop();
        Operand op_A,op_B;
        if(Operands.isEmpty())
        {
            PrintFile("Неправильная запись выражения: \""+c+Operands.peek().id+"\"");
            throw new Exception("Неправильная запись выражения: \""+c+Operands.peek().id+"\"");
        }
        op_B=(Operand)Operands.pop();
        B = (boolean)op_B.value;
        if(Operands.isEmpty())
        {
            PrintFile("Неправильная запись выражения: \""+c+Operands.peek().id+"\"");
            throw new Exception("Неправильная запись выражения: \""+c+op_B.id+"\"");
        }
        op_A=(Operand)Operands.pop();
        A = (boolean)op_A.value;
        //Формирование дерева
        if(op_A.ownlink==-1)
            treeList.add(new Tree(op_A.id,n_a=++numLink));
        if(op_B.ownlink==-1)
            treeList.add(new Tree(op_B.id,n_b=++numLink));
        treeList.add(t = new Tree(""+c,++numLink));
        if(op_A.ownlink==-1)t.l_link=n_a;
        else t.l_link=op_A.ownlink;
        if(op_B.ownlink==-1)t.r_link=n_b;
        else t.r_link=op_B.ownlink;
		/*Выполнение операций*/
        if(c=='|')Operands.push(new Operand("("+op_A.id+" "+c+" "+op_B.id+")",A||B,numLink));
        if(c=='&')Operands.push(new Operand("("+op_A.id+" "+c+" "+op_B.id+")",A&&B,numLink));
        if(c=='^')Operands.push(new Operand("("+op_A.id+" "+c+" "+op_B.id+")",A^B,numLink));
        if(c=='!')Operands.push(new Operand("("+c+op_B.id+")",!B,numLink));
        if(c=='=')
        {
            Operands.push(new Operand("("+op_A.id+""+c+""+op_B.id+")",B,numLink));
            if(op_A.id.equals("true")||op_A.id.equals("false"))
            {
                PrintFile("Неправильная запись выражения: Нельзя изменять значение констант true и false!");
                throw new Exception("Неправильная запись выражения: Нельзя изменять значение констант true и false!");
            }
            int k=0;
            for(int i=0;i<listName.size();i++)
            {
                Name n = (Name)listName.get(i);
                if(n.id.equals(op_A.id)) {n.value=B; k++;}
            }
            if(k==0)
            {
                PrintFile("Неправильная запись выражения: Присваивание применяется только к единичным идентификаторам!");
                throw new Exception("Неправильная запись выражения: Присваивание применяется только к единичным идентификаторам!");
            }
        }
		/*Запись в файл*/
        PrintFile((j++)+". ("+op_A.id+" "+c+" "+op_B.id+") = "+Operands.peek().value);
    }
    /* Ф-я получения нового объекта */
    public Object getToken(String s, int []pos) throws Exception
    {
		/* Пока не конец строки или не точка с запятой, пропускаем пробелы */
        while(pos[0]<s.length()&&s.charAt(pos[0])!=';'&&Character.isWhitespace(s.charAt(pos[0])))pos[0]++;
		/* Если дошли  до конца строки  возваращаем null*/
        if (pos[0] == s.length())
            return null;
		/* Если дошли до ; возвращаем признак конца выражения "end" */
        if (s.charAt(pos[0]) == ';')
            return "end";
		/* Проверяем является ли потенциальный объект операндом либо ограничителем */
        if((s.charAt(pos[0])>='A'&&s.charAt(pos[0])<='Z')||(s.charAt(pos[0])>='a'&&s.charAt(pos[0])<='z')||(s.charAt(pos[0])>='0'&&s.charAt(pos[0])<='9'))
        {
            return getID(s,pos);
        }
        else
            return getFunction(s,pos);
    }
    /* Ф-я обработки цифровых и буквенных записей */
    public Object getID (String s, int []pos) throws Exception
    {
        String id = "";
        int k=0;
        boolean value=false;
		/* Пока не конец строки либо не прерывается численно-буквенная последовательность */
        while (pos[0] < s.length()&&(Character.isDigit(s.charAt(pos[0]))||Character.isLetter(s.charAt(pos[0]))))
        {
            id += s.charAt(pos[0]++);
        }
		/* Если обнаружена буквенная запись операторов, выполняем их преобразование */
        if(id.equals("or")) return '|';
        if(id.equals("xor"))return '^';
        if(id.equals("and"))return '&';
        if(id.equals("not"))return '!';
		/* Поиск найденного идентификатора в таблице имен */
        for(int i=0;i<listName.size();i++)
        {
            Name n = (Name)listName.get(i);
            if(n.id.equals(id)){k=1;value=n.value;}
        }
        if(k==0) /*Если не нашел в таблице имен, добавляем новое имя( либо ошибка) */
        {
            Name n = new Name(id, false);
            listName.add(n);
            //throw new Exception("Неправильная запись выражения: Операнд "+id+" не объявлен!");
        }
        System.out.print(" "+id+" ");
        return new Operand(id, value,-1);
    }
    /* Ф-я обработки ограничителей  */
    public char getFunction (String s, int []pos) //throws Exception
    {
        char c;
        // в данном случае все операции состоят из одного символа
        // но мы можем усложнить код добавив == && || mod div и ещё чегонить
        c=s.charAt(pos[0]++);
		/* Если есть двойная запись, преобразуем в одинарную */
        if(c==':'&&s.charAt(pos[0])=='=')
        {
            pos[0]++;
            c='=';
        }
        if(c=='&'&&s.charAt(pos[0])=='&')
        {
            pos[0]++;
            c='&';
        }
        if(c=='|'&&s.charAt(pos[0])=='|')
        {
            pos[0]++;
            c='|';
        }
        return c;
    }
    /* Ф-я вывода данных в файл */
    public void PrintFile(String s)
    {
        outStr+=s+'\n';
        tArea.setText(outStr);
        if(chBox.getState())
            try
            {
                file.write(s);
                file.newLine();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
    }
    /*paint*/
    public void paint(Graphics g)
    {
        g.setFont(new Font("Дерево", base, 20) );
        g.setColor(Color.blue);
        g.drawString("Дерево", 810, 60);
        g.drawRect(640,30,390,440);
        g.setFont(new Font(outStr, base, 14) );
        g.setColor(Color.black);
        int [] queue = new int[50];	// очередь
        int k=0;
        queue[0]=base;
        for(int j=0;j<=k;j++)
        {
            for(int i=0;i<treeList.size();i++)//Пробежка по дереву
            {
                Tree t0 = (Tree)treeList.get(i);
                if(queue[j]==t0.ownLink)//Поиск текущего узла
                {
                    if(t0.ownLink==base){t0.x=840;t0.y=450;}//Если корень
                    g.drawString(t0.name, t0.x,t0.y);//Рисуем найденный узел

                    System.out.println(t0.name+" "+t0.ownLink);
                    if(t0.l_link>0) //Если есть левая ветвь
                    {
                        queue[++k]=t0.l_link; //записывем в очередь левое ответвление
                        for(int l=0;l<treeList.size();l++)//Пробежка по дереву
                        {
                            Tree t1 = (Tree)treeList.get(l);
                            if(t0.l_link==t1.ownLink)//Если нашли
                            {
                                t1.x=t0.x-80+Math.abs((450-t0.y+30)/2);
                                //t1.x=t0.x+400/;//Вычисляем координату
                                t1.y=t0.y-20;//Вычисляем у
                                if(!t1.name.equals(""))g.drawLine(t0.x, t0.y-12, t1.x+5, t1.y+5);//Рисуем ветку
                            }
                        }
                    }
                    if(t0.r_link>0&&t0.r_link!=t0.l_link)
                    {
                        queue[++k]=t0.r_link;
                        for(int l=0;l<treeList.size();l++)
                        {
                            Tree t1 = (Tree)treeList.get(l);
                            if(t0.r_link==t1.ownLink)
                            {
                                t1.x=t0.x+80-Math.abs((450-t0.y+30)/2);
                                t1.y=t0.y-20;
                                if(!t1.name.equals(""))g.drawLine(t0.x, t0.y-12, t1.x+5, t1.y+5);
                            }
                        }
                    }

                }


            }

        }
    }
    /* Главная ф-я. Запуск программы */
    public static void main(String[] args)
    {
        Program p = new Program();
        p.setTitle("Logic recognizer");
        p.setSize(1040, 480);
        p.setResizable(false);
        p.setBackground(Color.lightGray);
        p.setVisible(true);
    }
}
class Tree
{
    int x,y,ownLink,l_link=-1,r_link=-1;
    String name;
    public Tree(String name,int num)
    {
        this.ownLink=num;
        this.name=name;
    }
}
/* Класс единичной записи таблицы имен */
class Name
{
    public String id;
    public Boolean value;
    public Name(String s, boolean v)
    {
        this.id=s;
        this.value=v;

    }
}
/* Класс единичной записи стека операндов */
class Operand
{
    public String id;
    public Boolean value;
    public int ownlink=-1;
    public Operand(String s, boolean v, int link)
    {
        this.id=s;
        this.value=v;
        this.ownlink=link;
    }
}


