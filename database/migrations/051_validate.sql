SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.ProductCombo', N'U') IS NOT NULL
    THROW 51000, 'ProductCombo must not exist', 1;
IF OBJECT_ID(N'dbo.ProductComboItem', N'U') IS NOT NULL
    THROW 51000, 'ProductComboItem must not exist', 1;
IF OBJECT_ID(N'dbo.SupportTicket', N'U') IS NOT NULL
    THROW 51000, 'SupportTicket must not exist', 1;
IF OBJECT_ID(N'dbo.Notification', N'U') IS NOT NULL
    THROW 51000, 'Notification must not exist', 1;
IF OBJECT_ID(N'dbo.NotificationReadReceipt', N'U') IS NOT NULL
    THROW 51000, 'NotificationReadReceipt must not exist', 1;
IF OBJECT_ID(N'dbo.ProductModifierGroup', N'U') IS NULL
    THROW 51000, 'ProductModifierGroup must exist', 1;
IF OBJECT_ID(N'dbo.ProductModifierOption', N'U') IS NULL
    THROW 51000, 'ProductModifierOption must exist', 1;

SELECT N'051 validation passed' AS validation_result;
