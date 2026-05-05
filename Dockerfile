# Use a imagem base do Maven
FROM maven:3.8.4-openjdk-11-slim

# Instala as dependências necessárias
RUN apt-get update && apt-get install -y \
    netcat \
    && rm -rf /var/lib/apt/lists/*

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos do projeto
COPY . .

# Baixa as dependências do Maven (isso é armazenado em cache no Docker)
RUN mvn dependency:go-offline -B

# Define o comando padrão (será sobrescrito pelo docker-compose)
CMD ["tail", "-f", "/dev/null"]
