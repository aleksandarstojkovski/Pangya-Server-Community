USE [pangya]
GO
/****** Objeto:  StoredProcedure [pangya].[ProcMakeUserBeta]    Data do Script: 08/08/2026 09:24:35 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROCEDURE [pangya].[ProcMakeUserBeta]
    @NomeCompleto nvarchar(100),
    @Birthday DATETIME = NULL,
    @Sexo SMALLINT,
    @Pergunta NVARCHAR(100),
    @Resposta NVARCHAR(120) = NULL,
    @email_in nvarchar(100),
    @id_in nvarchar(30),
    @pass_in nvarchar(40),
    @ip_in nvarchar(20),
    @Referrer_Code NCHAR(25) = NULL
AS 
BEGIN
    SET XACT_ABORT ON
    SET NOCOUNT ON

    DECLARE @IDUSER int = 0
    DECLARE @Inviter_UID INT = NULL; 
    DECLARE @Is_Invited BIT = 0;
    DECLARE @BetaIndex BIGINT = NULL;

    BEGIN TRANSACTION
        BEGIN TRY
            
            -- 🎯 1. TRAVA DE CONCORRÊNCIA ATÔMICA
            SELECT @IDUSER = [UID] FROM pangya.account WITH (XLOCK, SERIALIZABLE) WHERE ID = @id_in

            -- Se a primeira aba já criou o usuário, as abas atrasadas entram aqui,
            IF (@IDUSER > 0)
            BEGIN
                COMMIT TRANSACTION
                SELECT @IDUSER
                RETURN
            END

            -- 2. Executa a criação do usuário na tabela oficial (pangya.account)
            EXECUTE pangya.ProcNewUser @NomeCompleto, @Birthday, @Sexo, @Pergunta, 
                @Resposta, @id_in, @pass_in, @ip_in

            -- 3. Recupera o novo UID gerado
            SET @IDUSER = (SELECT [UID] FROM pangya.account WHERE ID = @id_in) 

            -- 4. Busca o BetaIndex do jogador e a lógica do Sistema de Amigos
            SELECT @BetaIndex = [index] FROM [pangya].[contas_beta] WHERE [LoginID] = @id_in;

            IF @Referrer_Code IS NOT NULL AND @Referrer_Code <> '' AND @Referrer_Code <> 'NULL'
            BEGIN
                SELECT @Inviter_UID = [uid] FROM [pangya].[contas_beta] WHERE [codigo] = @Referrer_Code;
                IF @Inviter_UID IS NOT NULL
                    SET @Is_Invited = 1;
            END

            -- 5. [SISTEMA DE AMIGOS / RECOMPENSA]
            IF (@Inviter_UID > 0 and @Is_Invited = 1)
            BEGIN 
                UPDATE pangya.account 
                SET Inviter_UID = @Inviter_UID, 
                    Invited = @Is_Invited  
                WHERE [UID] = @IDUSER;

                -- Evento de Boas-Vindas: 50.000 Pontos (Tipo 5)
                EXEC pangya.USP_WEB_EVENT_SHOP @IDUSER, 50000, 5;
                
                -- Bonificação do Padrinho
                IF EXISTS (SELECT 1 FROM pangya.pangya_point_event WHERE [UID] = @Inviter_UID)
                BEGIN
                    UPDATE pangya.pangya_point_event 
                    SET points = points + 5000
                    WHERE [UID] = @Inviter_UID; 
                END 
            END
            ELSE
            BEGIN
                UPDATE pangya.account 
                SET Inviter_UID = -1, 
                    Invited = 0  
                WHERE [UID] = @IDUSER;

                UPDATE pangya.pangya_point_event 
                SET points = 10000
                WHERE [UID] = @IDUSER;  
            END
             
            IF @BetaIndex IS NOT NULL
            BEGIN
                UPDATE [pangya].[contas_beta] 
                SET [finish_reg] = 1, [uid] = @IDUSER
                WHERE [index] = @BetaIndex;
            END

            COMMIT TRANSACTION
            
            -- Retorna o UID gerado com sucesso
            SELECT @IDUSER
            
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION
            SELECT 0
        END CATCH
END
