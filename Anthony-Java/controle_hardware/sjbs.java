package arquivos;

import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class sjbs {

    static String[] hardware = new String[6];
    static String [] valuesecurity = new String [3];

    static Timer tempTimer = new Timer(); // um único timer global

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        boolean rodando = true;

        System.out.println("Aditions For System");
        systema();
        security();
        //controlers();

        while (rodando) {

            System.out.println("\n=== MENU ===");
            System.out.println("1. Funcionalidades");
            System.out.println("2. Inserir Especificações");
            System.out.println("3. Gerar IP");
            System.out.println("4. Observatory");
            System.out.println("5. Sair");

            int op = sc.nextInt();
            sc.nextLine(); // limpar buffer

            switch (op) {
                case 1:
                    Funcionalidades();
                    break;

                case 2:
                    especificacoes();
                    break;

                case 3:
                    conexoes();
                    break;

                case 4:
                    observatory();
                    break;

                case 5:
                    System.out.println("Encerrando sistema...");
                    tempTimer.cancel();
                    rodando = false;
                    break;

                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    public static void Funcionalidades() {
        Scanner source = new Scanner(System.in);
        System.out.println("\n1. Desligar\n2. Reiniciar");
        int value = source.nextInt();

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            int count = (value == 1) ? 3 : 2;

            @Override
            public void run() {
                if (count > 0) {
                    System.out.println(count + "...");
                    count--;
                } else {
                    System.out.println((value == 1) ? "Desligando..." : "Reiniciando...");
                    timer.cancel();
                }
            }
        };

        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    public static void conexoes() {
        Random random = new Random();

        int[] numeros = new int[10];

        for (int i = 0; i < numeros.length; i++) {
            numeros[i] = random.nextInt(10);
        }

        System.out.print("Gerado o IP: ");
        for (int n : numeros) {
            System.out.print(n);
        }
        System.out.println();
    }

    public static void especificacoes() {
        Scanner sc = new Scanner(System.in);

        System.out.println("\nDigite as especificações:\n");

        System.out.print("Placa-mãe: ");
        hardware[0] = sc.nextLine();

        System.out.print("RAM: ");
        hardware[1] = sc.nextLine();

        System.out.print("SSD: ");
        hardware[2] = sc.nextLine();

        System.out.print("HD: ");
        hardware[3] = sc.nextLine();

        System.out.print("Processador: ");
        hardware[4] = sc.nextLine();

        System.out.print("Placa de vídeo: ");
        hardware[5] = sc.nextLine();
    }

    public static void observatory() {
        System.out.println("\n--- Observatory ---");
        System.out.println("Placa Mãe: " + hardware[0]);
        System.out.println("RAM: " + hardware[1]);
        System.out.println("SSD: " + hardware[2]);
        System.out.println("HD: " + hardware[3]);
        System.out.println("Processador: " + hardware[4]);
        System.out.println("Placa de vídeo: " + hardware[5]);
    }

    public static void controlers() {

        Random random = new Random();

        TimerTask task = new TimerTask() {

            double temperatura = 25.0;

            @Override
            public void run() {

                double variacao = (random.nextDouble() * 5) - 1;
                temperatura += variacao;

                System.out.printf("\n[Monitor] Temperatura: %.2f°C\n", temperatura);

                if (temperatura <= 45) {
                    System.out.println("[Monitor] Temperatura Normal.");
                } else if (temperatura <= 55) {
                    System.out.println("[Monitor] ALERTA!");
                } else {
                    System.out.println("[Monitor] RISCO CRÍTICO!");
                    desligamento();
                }
            }
        };

        tempTimer.scheduleAtFixedRate(task, 0, 10000);
    }

    public static void desligamento() {
        System.out.println("Desligamento automático por temperatura!");
    }

    public static void systema (){
        Scanner nome = new Scanner(System.in);
        System.out.println("Digite o nome do usuário: ");
        valuesecurity[0] = nome.nextLine();
        //----------------------------------//
        Scanner senha = new Scanner(System.in);
        System.out.println("Digite a senha do usuário: ");
        valuesecurity[1] = senha.nextLine();

        System.out.println("Nome de usuário: " + valuesecurity[0]);
        System.out.println("Senha do usuário: " + valuesecurity[1]);
    }

    public static void security (){
        Scanner a = new Scanner(System.in);
        System.out.println("Informe o nome de usuário: ");
        String valor = a.nextLine();
        //---------------------------//
        Scanner b = new Scanner(System.in);
        System.out.println("Informe a senha do usuário: ");
        String sgvalor = b.nextLine();

        if (valor.equals(valuesecurity[0]) && sgvalor.equals(valuesecurity[1])) {
            System.out.println("Entrando no sistema operacional.");
        } else {
            System.out.println("Nome de usuário ou senha incorretas.");
        }
    }

}