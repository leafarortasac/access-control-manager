Access Control Manager 🔐 (Identity Service)

O Access Control Manager (ACM) é um serviço de Identidade e Acesso (IAM) de alto desempenho. Ele atua como o núcleo de segurança, gerenciando autenticação, autorização e isolamento de dados em uma arquitetura Multi-tenant.

🚀 Diferenciais de Arquitetura Sênior

Java 21 & Virtual Threads: Implementação de processamento assíncrono para importação de dados massivos, utilizando Project Loom para garantir escalabilidade extrema com baixo consumo de recursos.

Isolamento Multi-tenant (Database Level): Garantia de integridade e autoria das operações. Cada registro (usuário, tenant, ticket) é escopado por um tenant_id, permitindo que o mesmo e-mail ou CPF coexista em empresas diferentes sem conflitos.

Spring Security & JWT: Emissão de tokens assinados com claims customizadas, incluindo rotação de Refresh Token e barreira de segurança para Troca de Senha Obrigatória (First Access).

Database Migrations (Flyway): Versionamento rigoroso do esquema do banco de dados PostgreSQL.

🛠️ Tecnologias e Ferramentas
Java 21 (Virtual Threads ativas)

Spring Boot 3.4.1

PostgreSQL 15 (Isolamento via Constraints Únicas Escopadas)

Flyway: Gestão de scripts SQL evolutivos.

OpenCSV: Engine de processamento de arquivos.

MapStruct: Mapeamento de DTOs de alta performance.

Lombok: Redução de boilerplate code.

📦 Como Instalar e Rodar

1. Pré-requisitos
   Certifique-se de ter o Docker e Docker Compose instalados.

2. Subindo o Ambiente (Docker Compose)
   O projeto está configurado para subir a aplicação e o banco de dados PostgreSQL automaticamente. Na raiz do projeto, execute:

Bash
docker-compose up -d --build

3. Scripts de Banco de Dados
   O Flyway executará automaticamente os scripts localizados em src/main/resources/db/migration:

V1__init.sql: Criação de Tenants e Usuários.

🧪 Guia de Testes e algumas validações (Bootstrap do Sistema)

Para validar o sistema, siga os passos abaixo.

🚩 Fase 1: Fundação & Bootstrap (Sem Token)
Criar Tenant A: POST /api/v1/tenants

      {
         "name": "Gastrolight", 
         "subdomain": "gastrolight", 
         "active": true, 
         "settings": {
            "theme": "dark"
         }
      }

Criar Usuário ADMIN da Gastrolight (Sem Token)

Endpoint: POST /api/v1/users

Use o tenantId gerado acima, no payload abaixo, substituindo por "{ID_TENANT_A}".

      {
         "fullName": "Yago Roberto", 
         "email": "yago.roberto.moraes@gastrolight.com.br", 
         "password": "123456", 
         "role": "ADMIN", 
         "tenantId": "{ID_TENANT_A}", 
         "cpf": "47084535403"
      }

🚩 Fase 2: Ciclo de Vida do Token & Segurança

Login Contextualizado: POST /api/v1/auth/login

      {
        "email": "yago.roberto.moraes@gastrolight.com.br", 
        "password": "123456",
        "subdomain": "gastrolight"
      }

Autorizar o Swagger

Agora que você tem o token, vamos "avisar" o serviço de que você está autorizado:

      exemplo:
      {
        "accessToken":   "eyJhbGciOiJIUzUxMiJ9.eyJ0ZW5hbnRfaWQiOiIxNjE4MDMwNy0wZDI5LTRkYTMtOGI5ZS0yYjhkMzU0MThkMzEiLCJmaXJzdF9hY2Nlc3MiOnRydWUsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwic3ViIjoieWFnby5yb2JlcnRvLm1vcmFlc0BnYXN0cm9saWdodC5jb20uYnIiLCJpYXQiOjE3NzI1Nzc4ODUsImV4cCI6MTc3MjU3ODc4NX0.lfhtl1mY0mF34D2q9m5c9M5MRP3nX4lC1QJn6agpjPBnYapHPvXhls6tf4gFvUoU25ferKTNAp6XlpivnhLP5g",
        "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJ0ZW5hbnRfaWQiOiIxNjE4MDMwNy0wZDI5LTRkYTMtOGI5ZS0yYjhkMzU0MThkMzEiLCJzdWIiOiJ5YWdvLnJvYmVydG8ubW9yYWVzQGdhc3Ryb2xpZ2h0LmNvbS5iciIsImlhdCI6MTc3MjU3Nzg4NSwiZXhwIjoxNzczMTgyNjg1fQ.N5irEftJhwZDhSMTl8ODBUFmRjIWC4FpT1p5pskYah1LntP-6GwF1FysVIZpSOjOxAghXQsB5Raf0SKT736lAw",
        "tokenType": "Bearer",
        "fullName": "Yago Roberto",
        "role": "ADMIN",
        "firstAccess": true
      }

Clique no botão Authorize (ícone de cadeado no topo da página).

No campo de texto, cole o token obtido no passo anterior.

Clique em Authorize e depois em Close.

Teste de Barreira: Tente listar usuários com o token recebido.

Resultado: 403 Forbidden (Exigência de troca de senha).

      {
        "timestamp": "2026-03-03T22:48:39.769382949",
        "status": 403,
        "error": "Forbidden",
        "message": "Necessário trocar a senha no primeiro acesso.",
        "path": "/api/v1/users",
        "code": "FORBIDDEN"
      }

Troca de Senha: POST /api/v1/auth/change-password.

      {
        "oldPassword": "123456",
        "newPassword": "654321"
      }

Após a troca, logue novamente e depois autorize o swagger. O novo token permitirá acesso total.

🚩 Fase 3: Importação Massiva & Virtual Threads

Upload CSV: POST /api/v1/users/import (Multipart Form Data).

Selecione o arquivo src/main/resources/usuarios_importacao.csv.

Validação Assíncrona: O retorno será 202 Accepted. Verifique os logs da aplicação:

[Async] [virtual-XX] Iniciando processamento...

Observe as threads virtuais processando 20 registros simultaneamente sem bloquear o sistema.

🚩 Fase 4: Isolamento Multi-tenant

Conflito de CPF: Tente criar um usuário em um Tenant diferente com o mesmo CPF.

O sistema deve permitir, validando que o isolamento por tenant_id nas Constraints Únicas está funcionando.

Blindagem Cross-Tenant: Com o token do Tenant A, tente acessar um userId do Tenant B.

Resultado: 404 Not Found.

📖 Documentação Interativa (Swagger)

Visualize e teste todos os endpoints em:

🔗 http://localhost:8080/swagger-ui/index.html

📡 Observabilidade

Os logs da aplicação são estruturados para facilitar o rastreamento em microsserviços:

requestId: Identificador único da requisição.

tenantId: Contexto da empresa logada.

Exemplo: 2026-03-01 22:00:00 INFO [req-550e8400] [tenant-ed9d34dc] Executando listagem de usuários.
