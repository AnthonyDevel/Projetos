# AnthLang

> **Semântica, desempenho e confiança.**

AnthLang é uma linguagem de programação experimental desenvolvida com foco em **tipagem estática, segurança, desempenho e uma sintaxe simples e expressiva**.

O projeto nasceu como um estudo prático sobre os fundamentos envolvidos na criação de uma linguagem de programação e de seu compilador, abrangendo conceitos como **análise léxica, parsing, AST, tipagem e geração de código**.

---

## 🚧 Status do projeto

**Em desenvolvimento — Compiler 0.0.1**

O compilador atualmente possui uma base funcional de análise léxica e sintática, com suporte inicial a tipos primitivos, declarações e construção de uma **Abstract Syntax Tree (AST)**.

Novos recursos estão sendo implementados gradualmente.

---

## 🎯 Objetivos

O AnthLang busca combinar simplicidade de uso com características voltadas para desenvolvimento de software moderno.

Principais objetivos:

* Tipagem estática e forte
* Inferência de tipos
* Sintaxe simples e legível
* Segurança
* Desempenho
* Programação procedural
* Programação orientada a objetos
* Programação imperativa
* Programação reflexiva
* Concorrência

---

## ✨ Exemplo

Um exemplo simples de código AnthLang:

```anth
library "system"

int idade = 18;

printf.out(idade);
```

A linguagem também possui mecanismos planejados para **inferência de tipos**, utilizando palavras-chave como `deduct` e `conclude`.

---

## 🔤 Extensões

Arquivos de código AnthLang podem utilizar as seguintes extensões:

```text
.al
.anth
.ant
```

---

## 🧠 Tipagem

AnthLang utiliza como princípio uma abordagem de **tipagem estática e forte**.

A linguagem possui tipos primitivos semelhantes aos encontrados em linguagens como C, C++ e Java, enquanto recursos de inferência permitem reduzir declarações redundantes quando apropriado.

Exemplo conceitual:

```anth
int idade = 18;
```

E, futuramente:

```anth
deduct idade = 18;
```

---

## 🏗️ Arquitetura do compilador

O compilador está sendo desenvolvido de forma modular.

A arquitetura atual inclui componentes como:

```text
AnthLang/
├── anth-compiler/
│   ├── src/
│   ├── lexer/
│   ├── parser/
│   ├── ast/
│   └── ...
│
├── examples/
│   └── ...
│
└── README.md
```

### Pipeline

O processo de compilação segue, de forma geral, a ideia:

```text
Código AnthLang
      │
      ▼
    Lexer
      │
      ▼
Tokens
      │
      ▼
    Parser
      │
      ▼
     AST
      │
      ▼
Análise semântica
      │
      ▼
  Compilação
```

---

## 🔍 Lexer

O lexer é responsável por transformar o código-fonte em uma sequência de **tokens**.

Por exemplo:

```anth
int idade = 18;
```

é convertido conceitualmente em algo semelhante a:

```text
INT
IDENTIFIER
ASSIGN
INTEGER
SEMICOLON
```

---

## 🌳 AST

Após a análise sintática, o código é representado através de uma **Abstract Syntax Tree (AST)**.

A AST permite que o compilador trabalhe com a estrutura e o significado do programa em vez de manipular apenas o texto original.

---

## 🛠️ Tecnologias

O compilador é desenvolvido principalmente utilizando:

* **C**
* **C17**
* Estruturas de dados próprias
* Lexer
* Parser
* AST

Compilação atual baseada em:

```bash
gcc -Wall -Wextra -std=c17 \
src/main.c lexer/lexer.c src/keywords.c \
-o anthc
```

---

## 📚 Filosofia da linguagem

O AnthLang foi projetado em torno de três ideias principais:

### Semântica

O código deve possuir significado claro e previsível.

### Desempenho

A linguagem deve buscar gerar programas eficientes sem transformar a experiência do desenvolvedor em algo excessivamente complexo.

### Confiança

Erros devem ser identificados o mais cedo possível, utilizando tipagem estática e análise durante a compilação.

> **Semântica, desempenho e confiança.**

---

## 🗺️ Roadmap

### Compilador

* [x] Lexer inicial
* [x] Sistema de tokens
* [x] Keywords
* [x] Parser inicial
* [x] AST inicial
* [x] Tipos primitivos
* [ ] Análise semântica
* [ ] Sistema completo de tipos
* [ ] Inferência de tipos
* [ ] Tratamento avançado de erros
* [ ] Geração de código
* [ ] Otimizações
* [ ] Runtime

### Linguagem

* [x] Declarações básicas
* [x] Tipos primitivos
* [x] Expressões iniciais
* [ ] Funções
* [ ] Estruturas de controle completas
* [ ] Arrays
* [ ] Structs
* [ ] Classes
* [ ] Interfaces
* [ ] Generics
* [ ] Concorrência
* [ ] Reflexão

---

## 🧪 Exemplos

Os exemplos e testes da linguagem podem ser encontrados no diretório:

```text
examples/
```

Um arquivo AnthLang possui, por exemplo:

```anth
library "system"

int idade = 18;

printf.out(idade);
```

---

## 📌 Por que criar uma linguagem?

O AnthLang também funciona como um projeto de estudo sobre **engenharia de compiladores e construção de linguagens de programação**.

O desenvolvimento envolve conceitos de:

* Compiladores
* Linguagens formais
* Análise léxica
* Análise sintática
* AST
* Sistemas de tipos
* Gerenciamento de memória
* Geração de código
* Arquitetura de software

---

## 👨‍💻 Autor

**Anthony Cavalcante**

Projeto desenvolvido como estudo e experimentação em desenvolvimento de linguagens de programação e compiladores.

---

⭐ **AnthLang — Semântica, desempenho e confiança.**
