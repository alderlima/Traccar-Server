# Guia de Contribuição

Obrigado por considerar contribuir para o JAR Server Android App! Este documento fornece diretrizes e instruções para contribuir.

## Como Contribuir

### Reportar Bugs

Antes de criar um relatório de bug, verifique a lista de issues, pois você pode descobrir que o bug já foi relatado. Ao criar um relatório de bug, inclua:

- **Título descritivo** para o issue
- **Descrição exata do comportamento observado**
- **Comportamento esperado**
- **Passos para reproduzir o problema**
- **Exemplos específicos** para demonstrar os passos
- **Screenshots ou GIFs** se possível
- **Seu ambiente**: versão do Android, modelo do dispositivo, etc.

### Sugerir Melhorias

Sugestões de melhorias são sempre bem-vindas! Ao criar uma sugestão:

- Use um **título descritivo**
- Forneça uma **descrição clara da melhoria**
- Liste **exemplos de como a melhoria funcionaria**
- Explique **por que essa melhoria seria útil**

### Pull Requests

- Preencha o template fornecido
- Siga os padrões de código do projeto
- Inclua testes apropriados
- Atualize a documentação conforme necessário
- Termine todos os arquivos com uma nova linha

## Padrões de Código

### Java

- Use **camelCase** para nomes de variáveis e métodos
- Use **PascalCase** para nomes de classes
- Use **UPPER_SNAKE_CASE** para constantes
- Mantenha as linhas com menos de 100 caracteres quando possível
- Use 4 espaços para indentação

### Exemplo:

```java
public class MyClass {
    private static final String CONSTANT_VALUE = "value";
    
    private String myVariable;
    
    public void myMethod() {
        // Implementation
    }
}
```

### XML

- Use 2 espaços para indentação
- Mantenha os atributos organizados logicamente
- Use nomes descritivos para IDs

### Exemplo:

```xml
<LinearLayout
    android:id="@+id/myLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    
    <!-- Content -->
    
</LinearLayout>
```

## Processo de Desenvolvimento

1. **Fork** o repositório
2. **Clone** seu fork localmente
3. **Crie uma branch** para sua feature (`git checkout -b feature/AmazingFeature`)
4. **Commit** suas mudanças (`git commit -m 'Add some AmazingFeature'`)
5. **Push** para a branch (`git push origin feature/AmazingFeature`)
6. **Abra um Pull Request**

## Checklist para Pull Requests

- [ ] Meu código segue o estilo de código do projeto
- [ ] Eu executei um build local com sucesso
- [ ] Meu PR não duplica outro PR aberto
- [ ] Eu atualizei a documentação conforme necessário
- [ ] Minhas mudanças não geram novos warnings

## Estrutura de Commits

Use mensagens de commit claras e descritivas:

```
[TIPO] Descrição breve

Descrição mais detalhada se necessário.

Fixes #123
```

### Tipos de Commit:

- **feat**: Uma nova feature
- **fix**: Uma correção de bug
- **docs**: Mudanças na documentação
- **style**: Mudanças que não afetam o código (formatting, etc.)
- **refactor**: Refatoração de código sem mudanças de funcionalidade
- **perf**: Melhorias de performance
- **test**: Adição ou modificação de testes
- **chore**: Mudanças em build, dependências, etc.

## Dúvidas?

Sinta-se livre para abrir uma issue com a tag `question` ou entrar em contato através das issues.

---

Obrigado por contribuir! 🎉
