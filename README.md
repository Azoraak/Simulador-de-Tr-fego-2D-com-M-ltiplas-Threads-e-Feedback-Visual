Projeto desenvolvido para a matéria de Jogos para Consoles, focado em programação concorrente e sincronização de processos em Java.

A ideia é simular o tráfego em tempo real num cruzamento de cidade de forma autônoma, usando uma interface gráfica simples criada com Java Swing.

O que o projeto faz?
Cada carro é uma Thread: Cada veículo que aparece no mapa roda de forma totalmente independente, com a sua própria velocidade e direção.

Gerador Contínuo de Tráfego: Uma thread dedicada fica criando novos carros continuamente durante a execução.

Cruzamento Inteligente (Exclusão Mútua): Usamos a classe Semaphore para garantir que os carros não batam no meio do cruzamento e respeitem a ordem de chegada.

Semáforo Automático: Uma thread exclusiva controla o ciclo dos sinais (Verde, Amarelo e Vermelho) para organizar o trânsito das vias Norte-Sul e Leste-Oeste.

Visual em Tempo Real: Renderização suave a 60 FPS com vias de faixa dupla e semáforos interativos.

Tecnologias Usadas
Linguagem: Java

Interface Gráfica: Java Swing / Graphics2D

Concorrência: Threads, Runnables e Semáforos (java.util.concurrent)

IDE: Eclipse
